using System.ComponentModel;
using System.Diagnostics;
using System.Globalization;
using System.IO;
using System.Windows;
using System.Windows.Automation.Peers;
using System.Windows.Controls;
using System.Windows.Threading;
using Microsoft.Win32;

namespace MidnightProofStation;

public partial class MainWindow : Window
{
    private readonly StationBackend backend = new();
    private readonly DispatcherTimer timer = new() { Interval = TimeSpan.FromSeconds(8) };
    private string caPath = "";
    private bool refreshing;
    private bool servicesBusy;
    private bool proofBusy;
    private bool closing;
    private ProofMode SelectedProofMode => CudaMode.IsChecked == true ? ProofMode.Cuda : ProofMode.Cpu;

    public MainWindow() : this(false) { }

    internal MainWindow(bool capturePreview, bool captureCuda = false)
    {
        InitializeComponent();
        if (capturePreview)
        {
            if (captureCuda) CudaMode.IsChecked = true;
            return;
        }
        timer.Tick += async (_, _) => await RefreshAsync();
        Loaded += async (_, _) =>
        {
            timer.Start();
            await RefreshAsync();
        };
        Closing += (_, args) =>
        {
            if (!(servicesBusy || proofBusy)) return;
            args.Cancel = true;
            SetMessage("Wait for the current operation to finish before closing Proof Station.");
        };
        Closed += (_, _) =>
        {
            closing = true;
            timer.Stop();
            backend.Dispose();
        };
    }

    private async void StartPreview_Click(object sender, RoutedEventArgs e)
    {
        string uri = DatabaseUrl.Password.Trim();
        if (uri.Length == 0)
        {
            DatabaseSettings.IsExpanded = true;
            DatabaseSettings.UpdateLayout();
            DatabaseUrl.Focus();
            DatabaseUrl.BringIntoView();
            SetMessage("Enter the PostgreSQL URI for a synced Cardano Preview db-sync database.");
            return;
        }

        await RunServicesAsync("Starting Preview services", () => backend.StartPreviewAsync(uri, caPath, line =>
        {
            Log(line.Replace(uri, "[database URI hidden]", StringComparison.Ordinal));
        }));
    }

    private async void StartWallet_Click(object sender, RoutedEventArgs e) =>
        await RunServicesAsync("Starting the wallet prover", () => backend.StartWalletProverAsync(Log));

    private async void StopStack_Click(object sender, RoutedEventArgs e) =>
        await RunServicesAsync("Stopping Preview services", () => backend.StopAsync(Log));

    private async void Refresh_Click(object sender, RoutedEventArgs e) => await RefreshAsync();

    private void SelectCa_Click(object sender, RoutedEventArgs e)
    {
        var dialog = new OpenFileDialog { Filter = "PEM certificates|*.pem;*.crt|All files|*.*" };
        if (dialog.ShowDialog(this) != true) return;
        caPath = dialog.FileName;
        CaLabel.Text = Path.GetFileName(caPath);
        CaLabel.ToolTip = caPath;
    }

    private void ProofMode_Changed(object sender, RoutedEventArgs e)
    {
        if (BuildProver is null) return;
        bool cuda = SelectedProofMode == ProofMode.Cuda;
        CpuCard.Visibility = cuda ? Visibility.Collapsed : Visibility.Visible;
        GpuCard.Visibility = CheckGpu.Visibility = cuda ? Visibility.Visible : Visibility.Collapsed;
        BuildProver.Content = cuda ? "Build CUDA backend" : "Build CPU prover";
        SetProofState("No proof result", cuda
            ? "Detect the GPU and build the CUDA backend for its architecture."
            : "Build the CPU prover once, then generate sample proofs.");
    }

    private void DeviceIndex_Changed(object sender, TextChangedEventArgs e)
    {
        if (GpuStatus is null || ProofMetrics is null) return;
        GpuStatus.Text = "GPU not checked";
        GpuDetail.Text = "CUDA through your default WSL2 distribution";
        SetProofState("No proof result for this device", "Run sample proofs to check CUDA execution and stock verification.");
    }

    private async void CheckGpu_Click(object sender, RoutedEventArgs e) =>
        await RunProofAsync(ProofMode.Cuda, "Detecting the NVIDIA GPU", async index =>
        {
            var device = await backend.DetectGpuAsync(index);
            GpuStatus.Text = device.Name;
            GpuDetail.Text = $"sm_{device.Architecture} · {device.FreeMemory} / {device.TotalMemory} MiB free · Driver {device.Driver}";
        });

    private async void BuildProver_Click(object sender, RoutedEventArgs e)
    {
        var mode = SelectedProofMode;
        string prover = mode == ProofMode.Cpu ? "CPU prover" : "CUDA backend";
        await RunProofAsync(mode, $"Building the {prover}", async index =>
        {
            await backend.BuildProverAsync(mode, index, Log);
            SetProofState($"{prover} built", "Generate sample proofs to check both proofs with the stock verifier.");
        });
    }

    private async void GenerateProofs_Click(object sender, RoutedEventArgs e)
    {
        var mode = SelectedProofMode;
        string label = mode == ProofMode.Cpu ? "CPU" : "CUDA";
        await RunProofAsync(mode, $"Generating {label} sample proofs", async index =>
        {
            SetProofState($"{label} proof job running", "Producing a cold and warm proof for the stock verifier.");
            try
            {
                var result = await backend.ProveAsync(mode, index, Log);
                string verificationTime = $"{result.VerifySeconds * 1000:0.00} ms";
                ProofResult.Text = $"{result.Count} {label} proofs verified";
                WarmProofTime.Text = $"{result.WarmSeconds:0.000} s";
                if (mode == ProofMode.Cuda)
                {
                    ProofResultDetail.Text = $"Cold {result.ColdSeconds:0.000} s · warm verify {verificationTime} · {result.Fallbacks:N0} CPU fallbacks";
                    SecondMetricLabel.Text = "CUDA calls";
                    SecondMetricValue.Text = $"{result.DeviceCalls:N0}";
                    ThirdMetricLabel.Text = "Peak VRAM";
                    ThirdMetricValue.Text = $"{result.PeakBytes / 1048576d:N1} MiB";
                    ThirdMetricValue.ToolTip = "Peak device memory tracked by the CUDA prover.";
                }
                else
                {
                    ProofResultDetail.Text = "The stock verifier accepted both CPU proofs.";
                    SecondMetricLabel.Text = "Cold proof";
                    SecondMetricValue.Text = $"{result.ColdSeconds:0.000} s";
                    ThirdMetricLabel.Text = "Warm verify";
                    ThirdMetricValue.Text = verificationTime;
                    ThirdMetricValue.ToolTip = null;
                }
                ProofMetrics.Visibility = Visibility.Visible;
                Log($"{ProofResult.Text} · cold {result.ColdSeconds:0.000} s · warm {result.WarmSeconds:0.000} s · warm verify {verificationTime}.");
            }
            catch
            {
                SetProofState($"{label} proof job failed", "See the activity log for the reason.");
                throw;
            }
        });
    }

    private void SetProofState(string title, string detail)
    {
        ProofMetrics.Visibility = Visibility.Collapsed;
        ProofResult.Text = title;
        ProofResultDetail.Text = detail;
    }

    private void CopyAda_Click(object sender, RoutedEventArgs e) => CopyAddress(AdaAddress, "Cardano");

    private void CopyMidnight_Click(object sender, RoutedEventArgs e) => CopyAddress(MidnightAddress, "Midnight");

    private void CopyAddress(TextBox field, string network)
    {
        string feedback;
        try
        {
            Clipboard.SetText(field.Text);
            feedback = $"{network} address copied.";
        }
        catch (System.Runtime.InteropServices.ExternalException)
        {
            field.Focus();
            field.SelectAll();
            feedback = $"{network} address selected. Press Ctrl+C to copy.";
        }
        CopyFeedback.Text = feedback;
        SetMessage(feedback);
    }

    private void Profile_Click(object sender, RoutedEventArgs e) =>
        Process.Start(new ProcessStartInfo("https://x.com/kshot9000") { UseShellExecute = true });

    private async Task RunServicesAsync(string title, Func<Task> operation)
    {
        if (servicesBusy) return;
        servicesBusy = true;
        StartPreview.IsEnabled = StartWallet.IsEnabled = StopStack.IsEnabled = false;
        SetMessage(title + "…");
        Log(title + ".");
        try
        {
            await operation();
            SetMessage("Service command completed. Check the cards for readiness.");
        }
        catch (Exception error)
        {
            ReportError(error, "Start Docker Desktop in Linux container mode and check the local configuration.",
                DatabaseUrl.Password.Trim());
        }
        finally
        {
            servicesBusy = false;
            StartPreview.IsEnabled = StartWallet.IsEnabled = StopStack.IsEnabled = true;
            await RefreshAsync();
        }
    }

    private async Task RunProofAsync(ProofMode mode, string title, Func<int, Task> operation)
    {
        if (proofBusy) return;
        int index = 0;
        if (mode == ProofMode.Cuda && !int.TryParse(DeviceIndex.Text, NumberStyles.None, CultureInfo.InvariantCulture, out index))
        {
            SetMessage("Enter a numeric NVIDIA device index, such as 0.");
            DeviceIndex.Focus();
            return;
        }
        proofBusy = true;
        ProofModePicker.IsEnabled = CheckGpu.IsEnabled = BuildProver.IsEnabled = GenerateProofs.IsEnabled = DeviceIndex.IsEnabled = false;
        SetMessage(title + "…");
        Log(title + ".");
        try
        {
            await operation(index);
            SetMessage(title + " completed.");
        }
        catch (Exception error)
        {
            ReportError(error, "Install WSL2 and select a default Linux distribution. See the setup guide in the README.");
        }
        finally
        {
            proofBusy = false;
            ProofModePicker.IsEnabled = CheckGpu.IsEnabled = BuildProver.IsEnabled = GenerateProofs.IsEnabled = DeviceIndex.IsEnabled = true;
        }
    }

    private async Task RefreshAsync()
    {
        if (refreshing || closing) return;
        refreshing = true;
        RefreshStatus.IsEnabled = false;
        try
        {
            var status = await backend.RefreshAsync();
            if (closing) return;
            EngineStatus.Text = status.Docker;
            ShowStatus(NodeStatus, NodeDetail, status.Node);
            ShowStatus(IndexerStatus, IndexerDetail, status.Indexer);
            ShowStatus(ProofServerStatus, ProofServerDetail, status.ProofServer);
        }
        catch (Exception)
        {
            if (closing) return;
            EngineStatus.Text = "Start Docker Desktop in Linux container mode and refresh.";
            var unknown = new ServiceStatus("Status unavailable", "");
            ShowStatus(NodeStatus, NodeDetail, unknown);
            ShowStatus(IndexerStatus, IndexerDetail, unknown);
            ShowStatus(ProofServerStatus, ProofServerDetail, unknown);
        }
        finally
        {
            refreshing = false;
            if (!closing) RefreshStatus.IsEnabled = true;
        }
    }

    private static void ShowStatus(TextBlock state, TextBlock detail, ServiceStatus service)
    {
        state.Text = service.State;
        detail.Text = service.Detail;
    }

    private void SetMessage(string message)
    {
        Message.Text = message;
        (UIElementAutomationPeer.FromElement(Message) ?? UIElementAutomationPeer.CreatePeerForElement(Message))
            ?.RaiseAutomationEvent(AutomationEvents.LiveRegionChanged);
    }

    private void ReportError(Exception error, string missingToolHelp, string sensitive = "")
    {
        string detail = error is Win32Exception ? missingToolHelp : error.Message;
        if (sensitive.Length > 0) detail = detail.Replace(sensitive, "[database URI hidden]", StringComparison.Ordinal);
        Log(detail);
        SetMessage(detail);
    }

    private void Log(string entry)
    {
        if (string.IsNullOrWhiteSpace(entry)) return;
        if (!Dispatcher.CheckAccess())
        {
            Dispatcher.BeginInvoke(new Action(() => Log(entry)));
            return;
        }
        if (closing) return;
        if (entry.Length > 1200) entry = entry[..1200];
        ActivityLog.AppendText($"\n{DateTime.Now:HH:mm:ss}  {entry}");
        if (ActivityLog.Text.Length > 18000) ActivityLog.Text = ActivityLog.Text[^14000..];
        ActivityLog.ScrollToEnd();
    }
}
