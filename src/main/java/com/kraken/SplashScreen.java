package com.kraken;

import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;


@Slf4j
public class SplashScreen extends JFrame implements ActionListener {
    private static final Color BRAND_GREEN = new Color(105, 163, 60, 255);
    private static final Color DARKER_GRAY_COLOR = new Color(30, 30, 30);

    private static final int WIDTH = 200;
    private static final int PAD = 10;

    private static SplashScreen INSTANCE;

    private final JLabel action = new JLabel("Loading");
    private final JProgressBar progress = new JProgressBar();
    private final JLabel subAction = new JLabel();
    private final Timer timer;

    private volatile double overallProgress = 0;
    private volatile String actionText = "Loading";
    private volatile String subActionText = "";
    private volatile String progressText = null;

    private SplashScreen() throws IOException {
        setTitle("Kraken Launcher");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setUndecorated(true);
        BufferedImage logo = loadImageResource(KrakenLauncher.class, "com/kraken/images/kraken.png");

        log.info("Logo: " + logo);

        setIconImage(logo);

        setLayout(null);
        Container pane = getContentPane();
        pane.setBackground(DARKER_GRAY_COLOR);

        Font font = new Font(Font.DIALOG, Font.PLAIN, 12);
        JLabel logoLabel = new JLabel(new ImageIcon(logo));
        pane.add(logoLabel);
        logoLabel.setBounds(0, 0, WIDTH, WIDTH);

        int y = WIDTH;

        pane.add(action);
        action.setForeground(Color.WHITE);
        action.setBounds(0, y, WIDTH, 16);
        action.setHorizontalAlignment(SwingConstants.CENTER);
        action.setFont(font);
        y += action.getHeight() + PAD;

        pane.add(progress);
        progress.setForeground(BRAND_GREEN);
        progress.setBackground(BRAND_GREEN.darker().darker());
        progress.setBorder(new EmptyBorder(0, 0, 0, 0));
        progress.setBounds(0, y, WIDTH, 14);
        progress.setFont(font);
        progress.setUI(new BasicProgressBarUI()
        {
            @Override
            protected Color getSelectionBackground()
            {
                return Color.BLACK;
            }

            @Override
            protected Color getSelectionForeground()
            {
                return Color.BLACK;
            }
        });
        y += 12 + PAD;

        pane.add(subAction);
        subAction.setForeground(Color.LIGHT_GRAY);
        subAction.setBounds(0, y, WIDTH, 16);
        subAction.setHorizontalAlignment(SwingConstants.CENTER);
        subAction.setFont(font);
        y += subAction.getHeight() + PAD;

        setSize(WIDTH, y);
        setLocationRelativeTo(null);

        timer = new Timer(100, this);
        timer.setRepeats(true);
        timer.start();

        setVisible(true);
    }

    private static BufferedImage loadImageResource(Class<?> c, String path) {
        try (InputStream in = c.getResourceAsStream(path)) {
            synchronized(ImageIO.class) {
                return ImageIO.read(in);
            }
        } catch (IllegalArgumentException e) {
            String filePath;
            if (path.startsWith("/")) {
                filePath = path;
            } else {
                String var10000 = c.getPackage().getName().replace('.', '/');
                filePath = var10000 + "/" + path;
            }

            log.warn("Failed to load image from class: {}, path: {}", c.getName(), filePath);
            throw new IllegalArgumentException(path, e);
        } catch (IOException e) {
            throw new RuntimeException(path, e);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        action.setText(actionText);
        subAction.setText(subActionText);
        progress.setMaximum(1000);
        progress.setValue((int) (overallProgress * 1000));

        String progressText = this.progressText;
        if (progressText == null) {
            progress.setStringPainted(false);
        } else {
            progress.setStringPainted(true);
            progress.setString(progressText);
        }
    }

    public static void init() {
        try {
            SwingUtilities.invokeAndWait(() -> {
                if (INSTANCE != null) {
                    return;
                }

                try {
                    UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                    INSTANCE = new SplashScreen();
                }
                catch (Exception e) {
                    log.warn("Unable to start splash screen", e);
                }
            });
        }
        catch (InterruptedException | InvocationTargetException bs) {
            throw new RuntimeException(bs);
        }
    }

    public static void stop() {
        SwingUtilities.invokeLater(() -> {
            if (INSTANCE == null) {
                return;
            }

            INSTANCE.timer.stop();
            INSTANCE.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            INSTANCE.dispose();
            INSTANCE = null;
        });
    }

    public static void stage(double overallProgress, @Nullable String actionText, String subActionText) {
        stage(overallProgress, actionText, subActionText, null);
    }

    public static void stage(double startProgress, double endProgress,
                             @Nullable String actionText, String subActionText,
                             int done, int total, boolean mib) {
        String progress;
        if (mib) {
            final double MiB = 1024 * 1024;
            final double CEIL = 1.d / 10.d;
            progress = String.format("%.1f / %.1f MiB", done / MiB, (total / MiB) + CEIL);
        } else {
            progress = done + " / " + total;
        }
        stage(startProgress + ((endProgress - startProgress) * done / total), actionText, subActionText, progress);
    }

    public static void stage(double overallProgress, @Nullable String actionText, String subActionText, @Nullable String progressText) {
        if (INSTANCE != null) {
            INSTANCE.overallProgress = overallProgress;
            if (actionText != null) {
                INSTANCE.actionText = actionText;
            }
            INSTANCE.subActionText = subActionText;
            INSTANCE.progressText = progressText;
        }
    }
}
