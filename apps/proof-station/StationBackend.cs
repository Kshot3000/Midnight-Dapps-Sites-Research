using System.IO;
using System.Globalization;
using System.Net.Http;
using System.Net.Http.Json;
using System.Security.Cryptography;
using System.Text.Json;

namespace MidnightProofStation;

internal sealed record ServiceStatus(string State, string Detail);
internal sealed record StationStatus(string Docker, ServiceStatus Node, ServiceStatus Indexer, ServiceStatus ProofServer);
internal sealed record GpuDevice(string Name, string Architecture, string FreeMemory, string TotalMemory, string Driver);
internal sealed record GpuProof(string Count, string ColdSeconds, string WarmSeconds, string VerifySeconds,
    long DeviceCalls, long PeakBytes, long Fallbacks);

internal sealed class StationBackend : IDisposable
{
    private static readonly TimeSpan Quick = TimeSpan.FromSeconds(8);
    private readonly HttpClient http = new(new HttpClientHandler { UseProxy = false, AllowAutoRedirect = false })
    {
        Timeout = TimeSpan.FromSeconds(3)
    };
    private readonly string composeFile = Path.Combine(AppContext.BaseDirectory, "preview.compose.yaml");
    private readonly string caComposeFile = Path.Combine(AppContext.BaseDirectory, "db-tls.compose.yaml");
    private readonly string labScript = Path.Combine(AppContext.BaseDirectory, "gpu-lab.sh");
    private readonly string stateDirectory = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "MidnightProofStation");

    public async Task StartPreviewAsync(string databaseUrl, string caPath, Action<string> progress)
    {
        if (!Uri.TryCreate(databaseUrl, UriKind.Absolute, out var database) ||
            database.Scheme is not ("postgres" or "postgresql") ||
            string.IsNullOrWhiteSpace(database.Host) || database.AbsolutePath.Length < 2 ||
            databaseUrl.Any(char.IsControl))
            throw new ArgumentException("Enter the PostgreSQL URI for a synced Cardano Preview db-sync database.");

        if (caPath.Length > 0 && !File.Exists(caPath))
            throw new ArgumentException("Select an existing PEM file for the database CA.");

        await EnsureLocalEngineAsync();
        string secret = EnsureIndexerSecret();
        var result = await ComposeAsync(["up", "-d", "node", "indexer", "proof-server"],
            TimeSpan.FromMinutes(25), progress, databaseUrl, secret, caPath);
        result.RequireSuccess("Starting Preview services");
    }

    public async Task StartWalletProverAsync(Action<string> progress)
    {
        await EnsureLocalEngineAsync();
        var result = await ComposeAsync(["up", "-d", "--no-deps", "proof-server"],
            TimeSpan.FromMinutes(15), progress);
        result.RequireSuccess("Starting the wallet prover");
    }

    public async Task StopAsync(Action<string> progress)
    {
        await EnsureLocalEngineAsync();
        var result = await ComposeAsync(["stop"], TimeSpan.FromMinutes(2), progress);
        result.RequireSuccess("Stopping Preview services");
    }

    public async Task<StationStatus> RefreshAsync()
    {
        await EnsureLocalEngineAsync();
        var result = await ComposeAsync(["ps", "--status", "running", "--services"], Quick);
        result.RequireSuccess("Reading local service status");
        var running = result.Output.Split('\n', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries)
            .ToHashSet(StringComparer.Ordinal);
        var node = ProbeAsync(running, "node", ReadNodeAsync);
        var indexer = ProbeAsync(running, "indexer", ReadIndexerAsync);
        var prover = ProbeAsync(running, "proof-server", ReadProofServerAsync);
        await Task.WhenAll(node, indexer, prover);
        return new StationStatus("Docker Desktop connected", await node, await indexer, await prover);
    }

    public async Task<GpuDevice> DetectGpuAsync(int index)
    {
        var result = await GpuAsync("detect", index, TimeSpan.FromSeconds(40));
        result.RequireSuccess("Checking the WSL2 GPU");
        string[] parts = ReadRecord(result.Output, "STATION_GPU", 6);
        return new GpuDevice(parts[1], parts[2], parts[3], parts[4], parts[5]);
    }

    public async Task BuildGpuAsync(int index, Action<string> progress)
    {
        var result = await GpuAsync("build", index, TimeSpan.FromMinutes(90), progress);
        result.RequireSuccess("Building the CUDA draft");
        ReadRecord(result.Output, "STATION_READY", 2);
    }

    public async Task<GpuProof> ProveAsync(int index, Action<string> progress)
    {
        var result = await GpuAsync("prove", index, TimeSpan.FromMinutes(10), progress);
        result.RequireSuccess("Generating the sample proofs");
        string[] parts = ReadRecord(result.Output, "STATION_PROOF", 8);
        return new GpuProof(parts[1], parts[2], parts[3], parts[4],
            long.Parse(parts[5], CultureInfo.InvariantCulture), long.Parse(parts[6], CultureInfo.InvariantCulture),
            long.Parse(parts[7], CultureInfo.InvariantCulture));
    }

    private static string[] ReadRecord(string output, string prefix, int fields)
    {
        string? line = output.Split('\n').LastOrDefault(value => value.StartsWith(prefix + "\t", StringComparison.Ordinal));
        var parts = line?.TrimEnd('\r').Split('\t');
        return parts?.Length == fields ? parts : throw new InvalidOperationException("The CUDA lab did not return the expected result.");
    }

    private async Task<CommandResult> GpuAsync(string action, int index, TimeSpan timeout, Action<string>? progress = null)
    {
        string input = (await File.ReadAllTextAsync(labScript)).Replace("\r\n", "\n");
        return await CommandRunner.RunAsync("wsl.exe",
            ["--exec", "bash", "-s", "--", action, index.ToString(CultureInfo.InvariantCulture)],
            timeout, progress is null ? null : line =>
            {
                if (!line.StartsWith("STATION_", StringComparison.Ordinal)) progress(line);
            },
            input);
    }

    private static async Task EnsureLocalEngineAsync()
    {
        var result = await CommandRunner.RunAsync("docker.exe",
            ["context", "inspect", "desktop-linux", "--format", "{{.Endpoints.docker.Host}}"], Quick);
        result.RequireSuccess("Finding the Docker Desktop Linux engine");
        if (!string.Equals(result.Output.Trim(), "npipe:////./pipe/dockerDesktopLinuxEngine", StringComparison.OrdinalIgnoreCase))
            throw new InvalidOperationException("Select Docker Desktop’s local Linux engine. This app uses the standard desktop-linux context.");
    }

    private Task<CommandResult> ComposeAsync(string[] command, TimeSpan timeout, Action<string>? progress = null,
        string databaseUrl = "", string indexerSecret = "", string caPath = "")
    {
        var arguments = new List<string>
        {
            "--context", "desktop-linux", "compose", "--ansi", "never",
            "--project-name", "midnight-proof-station", "--file", composeFile
        };
        if (caPath.Length > 0) arguments.AddRange(["--file", caComposeFile]);
        arguments.AddRange(command);
        var variables = new Dictionary<string, string>
        {
            ["MIDNIGHT_STATION_DB_URL"] = databaseUrl,
            ["MIDNIGHT_STATION_INDEXER_SECRET"] = indexerSecret,
            ["MIDNIGHT_STATION_DB_CA"] = caPath
        };
        return CommandRunner.RunAsync("docker.exe", arguments, timeout, progress, environment: variables);
    }

    private string EnsureIndexerSecret()
    {
        Directory.CreateDirectory(stateDirectory);
        string path = Path.Combine(stateDirectory, "indexer-key");
        if (File.Exists(path)) return File.ReadAllText(path);
        string secret = Convert.ToHexString(RandomNumberGenerator.GetBytes(32)).ToLowerInvariant();
        string pending = path + "." + Guid.NewGuid().ToString("N") + ".tmp";
        try
        {
            File.WriteAllText(pending, secret);
            try { File.Move(pending, path); }
            catch (IOException) when (File.Exists(path)) { return File.ReadAllText(path); }
            return secret;
        }
        finally { File.Delete(pending); }
    }

    private static async Task<ServiceStatus> ProbeAsync(HashSet<string> running, string name, Func<Task<ServiceStatus>> probe)
    {
        if (!running.Contains(name)) return new ServiceStatus("Stopped", "No running container for this service.");
        try { return await probe(); }
        catch (Exception error) when (error is HttpRequestException or TaskCanceledException or JsonException
            or KeyNotFoundException or InvalidOperationException or FormatException)
        {
            return new ServiceStatus("Waiting for response", "The container is running. Its local API is still unavailable.");
        }
    }

    private async Task<ServiceStatus> ReadNodeAsync()
    {
        var chain = await RpcAsync("system_chain");
        if (!string.Equals(chain.GetString(), "Midnight Preview", StringComparison.OrdinalIgnoreCase))
            return new ServiceStatus("Unexpected network", "The local RPC did not identify itself as Midnight Preview.");

        var healthTask = RpcAsync("system_health");
        var headerTask = RpcAsync("chain_getHeader");
        await Task.WhenAll(healthTask, headerTask);
        var health = await healthTask;
        int peers = health.GetProperty("peers").GetInt32();
        bool syncing = health.GetProperty("isSyncing").GetBoolean();
        string number = (await headerTask).GetProperty("number").GetString() ?? throw new FormatException();
        if (!number.StartsWith("0x", StringComparison.Ordinal) ||
            !long.TryParse(number[2..], NumberStyles.HexNumber, CultureInfo.InvariantCulture, out long height))
            throw new FormatException("Unexpected block height.");
        string state = peers == 0 ? "Waiting for peers" : syncing ? "Syncing" : "Connected";
        return new ServiceStatus(state, $"Block {height:N0} · {peers:N0} peers");
    }

    private async Task<JsonElement> RpcAsync(string method)
    {
        using var response = await http.PostAsJsonAsync("http://127.0.0.1:9944",
            new { jsonrpc = "2.0", id = 1, method, @params = Array.Empty<int>() });
        response.EnsureSuccessStatusCode();
        await using var stream = await response.Content.ReadAsStreamAsync();
        using var document = await JsonDocument.ParseAsync(stream);
        return document.RootElement.GetProperty("result").Clone();
    }

    private async Task<ServiceStatus> ReadIndexerAsync()
    {
        using var response = await http.PostAsJsonAsync("http://127.0.0.1:8088/api/v4/graphql",
            new { query = "{ block { height } }" });
        response.EnsureSuccessStatusCode();
        await using var stream = await response.Content.ReadAsStreamAsync();
        using var document = await JsonDocument.ParseAsync(stream);
        var block = document.RootElement.GetProperty("data").GetProperty("block");
        return block.ValueKind == JsonValueKind.Null
            ? new("Indexing", "Waiting for the first indexed block.")
            : new("Responding", $"Indexed block {block.GetProperty("height").GetInt64():N0}");
    }

    private async Task<ServiceStatus> ReadProofServerAsync()
    {
        using var response = await http.GetAsync("http://127.0.0.1:6300/proof-versions");
        response.EnsureSuccessStatusCode();
        var versions = await response.Content.ReadFromJsonAsync<string[]>() ?? [];
        return versions.Contains("V3")
            ? new("Ready for wallet", $"Ledger 9 · {string.Join(" / ", versions)}")
            : new("Incompatible response", "The wallet prover did not advertise proof version V3.");
    }

    public void Dispose() => http.Dispose();
}
