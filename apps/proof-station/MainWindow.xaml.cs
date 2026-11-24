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
    private bool gpuBusy;
    private bool closing;

    public MainWindow() : this(false) { }

    internal MainWindow(bool capturePreview)
    {
        InitializeComponent();
        if (capturePreview) return;
        timer.Tick += async (_, _) => await RefreshAsync();
        Loaded += async (_, _) =>
        {
            timer.Start();
            await RefreshAsync();
        };
        Closing += (_, args) =>
        {
            if (!(servicesBusy || gpuBusy)) return;
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

    private void DeviceIndex_Changed(object sender, TextChangedEventArgs e)
    {
        if (GpuStatus is null || GpuResult is null) return;
        GpuStatus.Text = "GPU not checked";
        GpuDetail.Text = "CUDA through your default WSL2 distribution";
        SetProofState("No proof job run for this selection", "Run a sample job to check CUDA execution and stock verification.");
    }

    private async void CheckGpu_Click(object sender, RoutedEventArgs e) =>
        await RunGpuAsync("Detecting the NVIDIA GPU", async index =>
        {
            var device = await backend.DetectGpuAsync(index);
            GpuStatus.Text = device.Name;
            GpuDetail.Text = $"sm_{device.Architecture} · {device.FreeMemory} / {device.TotalMemory} MiB free · Driver {device.Driver}";
        });

    private async void BuildCuda_Click(object sender, RoutedEventArgs e) =>
        await RunGpuAsync("Building the pinned CUDA backend", async index =>
        {
            await backend.BuildGpuAsync(index, Log);
            SetProofState("CUDA backend built", "Generate sample proofs to confirm device execution.");
        });

    private async void GenerateProofs_Click(object sender, RoutedEventArgs e) =>
        await RunGpuAsync("Generating sample proofs", async index =>
        {
            SetProofState("Proof job running", "Checking a cold and warm proof with the stock verifier.");
            try
            {
                var result = await backend.ProveAsync(index, Log);
                GpuResult.Text = $"{result.Count} GPU proofs verified";
                GpuResultDetail.Text = $"Cold {result.ColdSeconds} s · verify {result.VerifySeconds} s · {result.Fallbacks:N0} CPU fallbacks";
                WarmProofTime.Text = $"{result.WarmSeconds} s";
                DeviceCallCount.Text = $"{result.DeviceCalls:N0}";
                PeakVram.Text = $"{result.PeakBytes / 1048576d:N1} MiB";
                ProofMetrics.Visibility = Visibility.Visible;
            }
            catch
            {
                SetProofState("Proof job failed", "See the activity log for the reason.");
                throw;
            }
        });

    private void SetProofState(string title, string detail)
    {
        ProofMetrics.Visibility = Visibility.Collapsed;
        GpuResult.Text = title;
        GpuResultDetail.Text = detail;
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

    private async Task RunGpuAsync(string title, Func<int, Task> operation)
    {
        if (gpuBusy) return;
        if (!int.TryParse(DeviceIndex.Text, NumberStyles.None, CultureInfo.InvariantCulture, out int index))
        {
            SetMessage("Enter a numeric NVIDIA device index, such as 0.");
            return;
        }
        gpuBusy = true;
        CheckGpu.IsEnabled = BuildCuda.IsEnabled = GenerateProofs.IsEnabled = DeviceIndex.IsEnabled = false;
        SetMessage(title + "…");
        Log(title + ".");
        try
        {
            await operation(index);
            SetMessage(title + " completed.");
        }
        catch (Exception error)
        {
            ReportError(error, "Check WSL2, your NVIDIA driver, and the CUDA prerequisites in the README.");
        }
        finally
        {
            gpuBusy = false;
            CheckGpu.IsEnabled = BuildCuda.IsEnabled = GenerateProofs.IsEnabled = DeviceIndex.IsEnabled = true;
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
