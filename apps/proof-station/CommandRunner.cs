using System.IO;
using System.Diagnostics;
using System.Text;

namespace MidnightProofStation;

internal sealed record CommandResult(int ExitCode, string Output, string Error)
{
    public void RequireSuccess(string action)
    {
        if (ExitCode != 0)
        {
            string detail = Error.Trim();
            if (detail.Length > 700) detail = detail[^700..];
            throw new InvalidOperationException(string.IsNullOrWhiteSpace(detail)
                ? $"{action} failed (exit {ExitCode})."
                : $"{action} failed. {detail}");
        }
    }
}

internal static class CommandRunner
{
    private static readonly Encoding Utf8 = new UTF8Encoding(false);

    public static async Task<CommandResult> RunAsync(
        string executable, IEnumerable<string> arguments, TimeSpan timeout,
        Action<string>? progress = null, string? input = null,
        IReadOnlyDictionary<string, string>? environment = null)
    {
        using var process = new Process
        {
            StartInfo = new ProcessStartInfo(executable)
            {
                UseShellExecute = false,
                CreateNoWindow = true,
                RedirectStandardOutput = true,
                RedirectStandardError = true,
                RedirectStandardInput = input is not null,
                StandardOutputEncoding = Utf8,
                StandardErrorEncoding = Utf8,
                WorkingDirectory = AppContext.BaseDirectory
            }
        };
        if (input is not null) process.StartInfo.StandardInputEncoding = Utf8;
        foreach (string argument in arguments) process.StartInfo.ArgumentList.Add(argument);
        if (environment is not null)
        {
            foreach (var (name, value) in environment) process.StartInfo.Environment[name] = value;
        }

        process.Start();
        using var deadline = new CancellationTokenSource(timeout);
        var output = CaptureAsync(process.StandardOutput, progress, deadline.Token);
        var error = CaptureAsync(process.StandardError, progress, deadline.Token);

        try
        {
            if (input is not null)
            {
                try { await process.StandardInput.WriteAsync(input.AsMemory(), deadline.Token).ConfigureAwait(false); }
                catch (IOException) when (process.HasExited) { }
                finally { process.StandardInput.Close(); }
            }

            await process.WaitForExitAsync(deadline.Token).ConfigureAwait(false);
            return new CommandResult(process.ExitCode, await output.ConfigureAwait(false), await error.ConfigureAwait(false));
        }
        catch (OperationCanceledException)
        {
            if (!process.HasExited)
            {
                try { process.Kill(entireProcessTree: true); }
                catch (InvalidOperationException) when (process.HasExited) { }
            }
            throw new TimeoutException("The operation timed out. Refresh the service status before trying again.");
        }
    }

    private static async Task<string> CaptureAsync(StreamReader reader, Action<string>? progress, CancellationToken token)
    {
        var tail = new StringBuilder();
        while (await reader.ReadLineAsync(token).ConfigureAwait(false) is { } line)
        {
            tail.AppendLine(line);
            if (tail.Length > 30000) tail.Remove(0, tail.Length - 30000);
            progress?.Invoke(line);
        }
        return tail.ToString();
    }
}
