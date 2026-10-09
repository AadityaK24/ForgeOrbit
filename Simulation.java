
import javafx.animation.AnimationTimer;

public class Simulation
{
    private final OrbitCanvas orbitCanvas;
    private final AnimationTimer animationTimer;

    private double durationSeconds = 30.0;
    private double playbackSpeed = 1.0;

    private double elapsedSeconds = 0.0;
    private double progress = 0.0;

    private long lastFrameNanos = -1;

    private boolean running = false;

    public Simulation(OrbitCanvas canvas)
    {
        if (canvas == null)
        {
            throw new IllegalArgumentException(
                "Orbit canvas cannot be null."
            );
        }

        orbitCanvas = canvas;

        animationTimer = new AnimationTimer()
        {
            @Override
            public void handle(long now)
            {
                updateSimulation(now);
            }
        };
    }

    public void play()
    {
        if (running)
        {
            return;
        }

        if (orbitCanvas.getMission() == null)
        {
            orbitCanvas.setSimulationStatus(
                "AWAITING MISSION"
            );

            return;
        }

        if (progress >= 1.0)
        {
            reset();
        }

        running = true;
        lastFrameNanos = -1;

        orbitCanvas.setSimulationStatus("RUNNING");

        animationTimer.start();
    }

    public void pause()
    {
        if (!running)
        {
            return;
        }

        running = false;
        lastFrameNanos = -1;

        animationTimer.stop();

        if (progress >= 1.0)
        {
            orbitCanvas.setSimulationStatus(
                "TRANSFER COMPLETE"
            );
        }
        else
        {
            orbitCanvas.setSimulationStatus("PAUSED");
        }
    }

    public void reset()
    {
        running = false;
        lastFrameNanos = -1;

        animationTimer.stop();

        elapsedSeconds = 0.0;
        progress = 0.0;

        orbitCanvas.resetSimulation();
    }

    private void updateSimulation(long now)
    {
        if (!running)
        {
            return;
        }

        if (lastFrameNanos < 0)
        {
            lastFrameNanos = now;
            return;
        }

        double frameTime =
            (now - lastFrameNanos) / 1_000_000_000.0;

        lastFrameNanos = now;

        if (frameTime < 0.0)
        {
            frameTime = 0.0;
        }

        elapsedSeconds += frameTime * playbackSpeed;

        progress = Math.min(
            1.0,
            elapsedSeconds / durationSeconds
        );

        orbitCanvas.setProgress(progress);

        if (progress >= 1.0)
        {
            finishSimulation();
        }
    }

    private void finishSimulation()
    {
        running = false;
        progress = 1.0;
        elapsedSeconds = durationSeconds;
        lastFrameNanos = -1;

        animationTimer.stop();

        orbitCanvas.setProgress(1.0);

        orbitCanvas.setSimulationStatus(
            "TRANSFER COMPLETE"
        );
    }

    public void setDurationSeconds(double seconds)
    {
        if (!Double.isFinite(seconds) || seconds <= 0.0)
        {
            throw new IllegalArgumentException(
                "Simulation duration must be finite and greater than zero."
            );
        }

        durationSeconds = seconds;

        elapsedSeconds = progress * durationSeconds;
    }

    public double getDurationSeconds()
    {
        return durationSeconds;
    }

    public void setPlaybackSpeed(double speed)
    {
        if (!Double.isFinite(speed)
                || speed < 0.1
                || speed > 10.0)
        {
            throw new IllegalArgumentException(
                "Playback speed must be between 0.1x and 10x."
            );
        }

        playbackSpeed = speed;
    }

    public double getPlaybackSpeed()
    {
        return playbackSpeed;
    }

    public double getProgress()
    {
        return progress;
    }

    public double getElapsedSeconds()
    {
        return elapsedSeconds;
    }

    public double getRemainingSeconds()
    {
        return Math.max(
            0.0,
            durationSeconds - elapsedSeconds
        );
    }

    public boolean isRunning()
    {
        return running;
    }

    public boolean isComplete()
    {
        return progress >= 1.0;
    }

    public OrbitCanvas getOrbitCanvas()
    {
        return orbitCanvas;
    }

    public void dispose()
    {
        running = false;
        lastFrameNanos = -1;

        animationTimer.stop();
    }
}
