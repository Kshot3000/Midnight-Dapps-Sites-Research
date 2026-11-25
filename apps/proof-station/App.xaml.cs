using System.IO;
using System.Windows;
using System.Windows.Media;
using System.Windows.Media.Imaging;

namespace MidnightProofStation;

public partial class App : Application
{
    protected override void OnStartup(StartupEventArgs e)
    {
        base.OnStartup(e);
        bool captureCuda = e.Args is ["--capture-preview", _, "cuda"];
        bool capture = captureCuda || e.Args is ["--capture-preview", _];
        var window = new MainWindow(capture, captureCuda);
        MainWindow = window;

        if (capture)
        {
            window.ContentRendered += (_, _) =>
            {
                try
                {
                    window.VerifyPreviewLayout(captureCuda);
                    var view = (FrameworkElement)window.Content;
                    var bitmap = new RenderTargetBitmap(
                        (int)Math.Ceiling(view.ActualWidth), (int)Math.Ceiling(view.ActualHeight),
                        96, 96, PixelFormats.Pbgra32);
                    bitmap.Render(view);
                    var encoder = new PngBitmapEncoder();
                    encoder.Frames.Add(BitmapFrame.Create(bitmap));
                    using var file = File.Create(e.Args[1]);
                    encoder.Save(file);
                    Shutdown(0);
                }
                catch (Exception error)
                {
                    File.WriteAllText(e.Args[1] + ".error", error.ToString());
                    Shutdown(1);
                }
            };
        }

        window.Show();
    }
}
