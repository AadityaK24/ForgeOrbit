
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.geometry.VPos;

public class OrbitCanvas extends Canvas
{
    private Mission mission;
    private TransferResult transferResult;

    private double progress = 0.0;
    private String simulationStatus = "READY";

    private final Color backgroundColor = Color.web("#07111F");
    private final Color initialOrbitColor = Color.web("#35C6F4");
    private final Color targetOrbitColor = Color.web("#5686FF");
    private final Color transferOrbitColor = Color.web("#B58AFF");
    private final Color satelliteColor = Color.web("#FFC857");

    public OrbitCanvas()
    {
        this(760, 560);
    }

    public OrbitCanvas(double width, double height)
    {
        super(width, height);

        widthProperty().addListener(
            (observable, oldValue, newValue) -> redraw()
        );

        heightProperty().addListener(
            (observable, oldValue, newValue) -> redraw()
        );

        redraw();
    }

    public void setMission(Mission mission)
    {
        this.mission = mission;

        if (mission == null)
        {
            transferResult = null;
            progress = 0.0;
            simulationStatus = "READY";
        }
        else
        {
            transferResult = mission.calculateTransfer();
            progress = 0.0;
            simulationStatus = "READY";
        }

        redraw();
    }

    public Mission getMission()
    {
        return mission;
    }

    public void setTransferResult(TransferResult transferResult)
    {
        this.transferResult = transferResult;
        redraw();
    }

    public TransferResult getTransferResult()
    {
        return transferResult;
    }

    public void setProgress(double progress)
    {
        if (!Double.isFinite(progress))
        {
            throw new IllegalArgumentException(
                "Simulation progress must be finite."
            );
        }

        this.progress = Math.max(
            0.0,
            Math.min(1.0, progress)
        );

        if (this.progress >= 1.0)
        {
            simulationStatus = "TRANSFER COMPLETE";
        }
        else if (this.progress > 0.0)
        {
            simulationStatus = "TRANSFER IN PROGRESS";
        }
        else
        {
            simulationStatus = "READY";
        }

        redraw();
    }

    public double getProgress()
    {
        return progress;
    }

    public void setSimulationStatus(String status)
    {
        if (status == null || status.trim().isEmpty())
        {
            simulationStatus = "READY";
        }
        else
        {
            simulationStatus = status.trim().toUpperCase();
        }

        redraw();
    }

    public void resetSimulation()
    {
        progress = 0.0;
        simulationStatus = "READY";
        redraw();
    }

    public void redraw()
    {
        double width = getWidth();
        double height = getHeight();

        if (width < 100 || height < 100)
        {
            return;
        }

        GraphicsContext gc = getGraphicsContext2D();

        gc.setFill(backgroundColor);
        gc.fillRect(0, 0, width, height);

        drawStars(gc, width, height);
        drawHeader(gc, width);

        if (mission == null)
        {
            drawWaitingScene(gc, width, height);
            return;
        }

        drawMissionScene(gc, width, height);
    }

    private void drawHeader(
        GraphicsContext gc,
        double width)
    {
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.CENTER);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font(
            "Segoe UI",
            FontWeight.BOLD,
            22
        ));

        gc.fillText("ORBITAL TRAJECTORY", 24, 30);

        gc.setFill(Color.web("#91A8C4"));
        gc.setFont(Font.font("Segoe UI", 11));

        gc.fillText(
            "FORGEORBIT  /  MISSION VISUALIZATION",
            26,
            51
        );

        double pillWidth = Math.min(
            190,
            Math.max(100, simulationStatus.length() * 7.0 + 24)
        );

        double pillX = width - pillWidth - 24;

        gc.setFill(Color.rgb(35, 55, 80, 0.9));
        gc.fillRoundRect(
            pillX,
            18,
            pillWidth,
            28,
            14,
            14
        );

        gc.setStroke(Color.web("#3F6288"));
        gc.setLineWidth(1);

        gc.strokeRoundRect(
            pillX,
            18,
            pillWidth,
            28,
            14,
            14
        );

        gc.setFill(Color.web("#C8D8EC"));
        gc.setFont(Font.font(
            "Segoe UI",
            FontWeight.BOLD,
            10
        ));

        gc.setTextAlign(TextAlignment.CENTER);

        gc.fillText(
            simulationStatus,
            pillX + pillWidth / 2.0,
            32
        );

        gc.setTextAlign(TextAlignment.LEFT);
    }

    private void drawMissionScene(
        GraphicsContext gc,
        double width,
        double height)
    {
        Orbit initialOrbit = mission.getInitialOrbit();
        Orbit targetOrbit = mission.getTargetOrbit();

        double initialAltitudeKm =
            initialOrbit.getAltitude() / Constants.KM_TO_M;

        double targetAltitudeKm =
            targetOrbit.getAltitude() / Constants.KM_TO_M;

        double maximumAltitudeKm = Math.max(
            initialAltitudeKm,
            targetAltitudeKm
        );

        double minimumDimension = Math.min(width, height);

        double earthRadiusPixels = Math.min(
            92,
            minimumDimension * 0.135
        );

        double altitudeDisplayRange =
            minimumDimension * 0.225;

        // Altitude spacing is exaggerated for visual clarity.
        double initialRadius = earthRadiusPixels
            + (initialAltitudeKm / maximumAltitudeKm)
            * altitudeDisplayRange;

        double targetRadius = earthRadiusPixels
            + (targetAltitudeKm / maximumAltitudeKm)
            * altitudeDisplayRange;

        double centerX = width / 2.0;
        double centerY = height * 0.53;

        drawOrbit(
            gc,
            centerX,
            centerY,
            initialRadius,
            initialOrbitColor,
            false
        );

        drawOrbit(
            gc,
            centerX,
            centerY,
            targetRadius,
            targetOrbitColor,
            true
        );

        double transferCenterOffset =
            (targetRadius - initialRadius) / 2.0;

        double transferSemiMajorAxis =
            (initialRadius + targetRadius) / 2.0;

        double transferSemiMinorAxis = Math.sqrt(
            initialRadius * targetRadius
        );

        drawTransferOrbit(
            gc,
            centerX,
            centerY,
            transferCenterOffset,
            transferSemiMajorAxis,
            transferSemiMinorAxis
        );

        drawBurnMarkers(
            gc,
            centerX,
            centerY,
            initialRadius,
            targetRadius
        );

        drawEarth(
            gc,
            centerX,
            centerY,
            earthRadiusPixels
        );

        drawSatellite(
            gc,
            centerX,
            centerY,
            transferCenterOffset,
            transferSemiMajorAxis,
            transferSemiMinorAxis,
            initialRadius,
            targetRadius
        );

        drawLegend(
            gc,
            height,
            initialAltitudeKm,
            targetAltitudeKm
        );

        if (transferResult != null)
        {
            drawTransferInformation(
                gc,
                width,
                transferResult
            );
        }
    }

    private void drawOrbit(
        GraphicsContext gc,
        double centerX,
        double centerY,
        double radius,
        Color orbitColor,
        boolean dashed)
    {
        gc.save();

        gc.setStroke(orbitColor);
        gc.setLineWidth(dashed ? 1.6 : 2.0);

        if (dashed)
        {
            gc.setLineDashes(8, 6);
        }

        gc.strokeOval(
            centerX - radius,
            centerY - radius,
            radius * 2,
            radius * 2
        );

        gc.restore();
    }

    private void drawTransferOrbit(
        GraphicsContext gc,
        double centerX,
        double centerY,
        double centerOffset,
        double semiMajorAxis,
        double semiMinorAxis)
    {
        gc.save();

        gc.setStroke(transferOrbitColor);
        gc.setLineWidth(2.0);
        gc.setLineDashes(5, 5);

        gc.strokeOval(
            centerX + centerOffset - semiMajorAxis,
            centerY - semiMinorAxis,
            semiMajorAxis * 2,
            semiMinorAxis * 2
        );

        gc.restore();
    }

    private void drawBurnMarkers(
        GraphicsContext gc,
        double centerX,
        double centerY,
        double initialRadius,
        double targetRadius)
    {
        double firstBurnX;
        double secondBurnX;

        if (targetRadius >= initialRadius)
        {
            firstBurnX = centerX - initialRadius;
            secondBurnX = centerX + targetRadius;
        }
        else
        {
            firstBurnX = centerX + initialRadius;
            secondBurnX = centerX - targetRadius;
        }

        drawBurnMarker(
            gc,
            firstBurnX,
            centerY,
            "BURN 1"
        );

        drawBurnMarker(
            gc,
            secondBurnX,
            centerY,
            "BURN 2"
        );
    }

    private void drawBurnMarker(
        GraphicsContext gc,
        double x,
        double y,
        String label)
    {
        gc.setFill(Color.web("#FFC857"));
        gc.fillOval(x - 4, y - 4, 8, 8);

        gc.setStroke(Color.web("#FFC857"));
        gc.setLineWidth(1);

        gc.strokeLine(
            x,
            y - 9,
            x,
            y - 23
        );

        gc.setFill(Color.web("#FFE4A1"));
        gc.setFont(Font.font(
            "Segoe UI",
            FontWeight.BOLD,
            9
        ));

        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(label, x, y - 32);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    private void drawEarth(
        GraphicsContext gc,
        double centerX,
        double centerY,
        double radius)
    {
        // Atmospheric glow.
        gc.setFill(Color.rgb(32, 147, 235, 0.08));

        gc.fillOval(
            centerX - radius * 1.25,
            centerY - radius * 1.25,
            radius * 2.5,
            radius * 2.5
        );

        gc.setFill(Color.rgb(32, 147, 235, 0.12));

        gc.fillOval(
            centerX - radius * 1.12,
            centerY - radius * 1.12,
            radius * 2.24,
            radius * 2.24
        );

        gc.setFill(Color.web("#0B4779"));

        gc.fillOval(
            centerX - radius,
            centerY - radius,
            radius * 2,
            radius * 2
        );

        // Clip surface details to the Earth.
        gc.save();

        gc.beginPath();

        gc.arc(
            centerX,
            centerY,
            radius,
            radius,
            0,
            360
        );

        gc.closePath();
        gc.clip();

        gc.setFill(Color.web("#176E91"));

        gc.fillOval(
            centerX - radius * 0.75,
            centerY - radius * 0.75,
            radius * 1.7,
            radius * 1.5
        );

        gc.setFill(Color.web("#3EAB88"));

        drawRelativePolygon(
            gc,
            centerX,
            centerY,
            radius,
            new double[] {
                -0.78, -0.67, -0.43, -0.29,
                -0.34, -0.48, -0.62, -0.73
            },
            new double[] {
                -0.45, -0.69, -0.73, -0.51,
                -0.28, -0.16, -0.28, -0.30
            }
        );

        drawRelativePolygon(
            gc,
            centerX,
            centerY,
            radius,
            new double[] {
                -0.38, -0.20, -0.08, -0.15,
                -0.26, -0.37, -0.42
            },
            new double[] {
                0.02, 0.05, 0.24, 0.47,
                0.73, 0.47, 0.20
            }
        );

        drawRelativePolygon(
            gc,
            centerX,
            centerY,
            radius,
            new double[] {
                0.05, 0.22, 0.46, 0.69,
                0.72, 0.53, 0.37, 0.19
            },
            new double[] {
                -0.53, -0.66, -0.53, -0.30,
                -0.06, 0.04, -0.12, -0.20
            }
        );

        drawRelativePolygon(
            gc,
            centerX,
            centerY,
            radius,
            new double[] {
                0.12, 0.37, 0.43, 0.32,
                0.22, 0.08
            },
            new double[] {
                -0.04, 0.01, 0.25, 0.53,
                0.66, 0.32
            }
        );

        // Latitude lines.
        gc.setStroke(Color.rgb(144, 220, 248, 0.35));
        gc.setLineWidth(0.7);

        for (int i = -1; i <= 1; i++)
        {
            double fraction = i * 0.45;

            double y = centerY + fraction * radius;

            double halfWidth = radius * Math.sqrt(
                1.0 - fraction * fraction
            );

            gc.strokeLine(
                centerX - halfWidth,
                y,
                centerX + halfWidth,
                y
            );
        }

        gc.restore();

        gc.setStroke(Color.web("#59B5E8"));
        gc.setLineWidth(1.4);

        gc.strokeOval(
            centerX - radius,
            centerY - radius,
            radius * 2,
            radius * 2
        );
    }

    private void drawRelativePolygon(
        GraphicsContext gc,
        double centerX,
        double centerY,
        double radius,
        double[] xCoordinates,
        double[] yCoordinates)
    {
        double[] xPoints = new double[xCoordinates.length];
        double[] yPoints = new double[yCoordinates.length];

        for (int i = 0; i < xCoordinates.length; i++)
        {
            xPoints[i] = centerX + xCoordinates[i] * radius;
            yPoints[i] = centerY + yCoordinates[i] * radius;
        }

        gc.fillPolygon(
            xPoints,
            yPoints,
            xPoints.length
        );
    }

    private void drawSatellite(
        GraphicsContext gc,
        double centerX,
        double centerY,
        double centerOffset,
        double semiMajorAxis,
        double semiMinorAxis,
        double initialRadius,
        double targetRadius)
    {
        double angle = Math.PI * progress;

        double satelliteX;

        if (targetRadius >= initialRadius)
        {
            satelliteX = centerX
                + centerOffset
                - semiMajorAxis * Math.cos(angle);
        }
        else
        {
            satelliteX = centerX
                + centerOffset
                + semiMajorAxis * Math.cos(angle);
        }

        double satelliteY =
            centerY - semiMinorAxis * Math.sin(angle);

        // Satellite glow.
        gc.setFill(Color.rgb(255, 200, 87, 0.18));

        gc.fillOval(
            satelliteX - 12,
            satelliteY - 12,
            24,
            24
        );

        gc.setFill(satelliteColor);

        gc.fillOval(
            satelliteX - 5,
            satelliteY - 5,
            10,
            10
        );

        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1);

        gc.strokeOval(
            satelliteX - 5,
            satelliteY - 5,
            10,
            10
        );
    }

    private void drawTransferInformation(
        GraphicsContext gc,
        double width,
        TransferResult result)
    {
        double panelWidth = 190;
        double panelHeight = 62;
        double x = width - panelWidth - 22;
        double y = 70;

        gc.setFill(Color.rgb(13, 28, 47, 0.94));

        gc.fillRoundRect(
            x,
            y,
            panelWidth,
            panelHeight,
            12,
            12
        );

        gc.setStroke(Color.web("#344D6C"));
        gc.setLineWidth(1);

        gc.strokeRoundRect(
            x,
            y,
            panelWidth,
            panelHeight,
            12,
            12
        );

        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.CENTER);

        gc.setFill(Color.web("#91A8C4"));
        gc.setFont(Font.font("Segoe UI", 10));

        gc.fillText(
            "TOTAL TRANSFER Δv",
            x + 12,
            y + 17
        );

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font(
            "Segoe UI",
            FontWeight.BOLD,
            17
        ));

        gc.fillText(
            String.format(
                "%.2f m/s",
                result.getTotalDeltaV()
            ),
            x + 12,
            y + 42
        );
    }

    private void drawLegend(
        GraphicsContext gc,
        double height,
        double initialAltitudeKm,
        double targetAltitudeKm)
    {
        double y = height - 27;

        drawLegendItem(
            gc,
            24,
            y,
            initialOrbitColor,
            String.format(
                "INITIAL  %.0f km",
                initialAltitudeKm
            )
        );

        drawLegendItem(
            gc,
            205,
            y,
            targetOrbitColor,
            String.format(
                "TARGET  %.0f km",
                targetAltitudeKm
            )
        );

        drawLegendItem(
            gc,
            370,
            y,
            transferOrbitColor,
            "TRANSFER ORBIT"
        );

        drawLegendItem(
            gc,
            535,
            y,
            satelliteColor,
            "SPACECRAFT"
        );
    }

    private void drawLegendItem(
        GraphicsContext gc,
        double x,
        double y,
        Color color,
        String label)
    {
        gc.setFill(color);

        gc.fillOval(
            x,
            y - 4,
            8,
            8
        );

        gc.setFill(Color.web("#C1D0E3"));
        gc.setFont(Font.font("Segoe UI", 9));
        gc.setTextAlign(TextAlignment.LEFT);

        gc.fillText(
            label,
            x + 14,
            y
        );
    }

    private void drawWaitingScene(
        GraphicsContext gc,
        double width,
        double height)
    {
        double minimumDimension = Math.min(width, height);

        double earthRadius = Math.min(
            90,
            minimumDimension * 0.14
        );

        double centerX = width / 2.0;
        double centerY = height * 0.48;

        gc.setStroke(Color.rgb(53, 198, 244, 0.35));
        gc.setLineWidth(1.5);
        gc.setLineDashes(7, 7);

        double orbitRadius = earthRadius
            + minimumDimension * 0.22;

        gc.strokeOval(
            centerX - orbitRadius,
            centerY - orbitRadius,
            orbitRadius * 2,
            orbitRadius * 2
        );

        gc.setLineDashes();

        drawEarth(
            gc,
            centerX,
            centerY,
            earthRadius
        );

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font(
            "Segoe UI",
            FontWeight.BOLD,
            16
        ));

        gc.fillText(
            "AWAITING MISSION DATA",
            centerX,
            height - 62
        );

        gc.setFill(Color.web("#91A8C4"));
        gc.setFont(Font.font("Segoe UI", 11));

        gc.fillText(
            "Configure a satellite mission to display its trajectory.",
            centerX,
            height - 40
        );

        gc.setTextAlign(TextAlignment.LEFT);
    }

    private void drawStars(
        GraphicsContext gc,
        double width,
        double height)
    {
        double[][] stars = {
            {0.08, 0.15}, {0.17, 0.28}, {0.28, 0.11},
            {0.39, 0.20}, {0.55, 0.13}, {0.68, 0.29},
            {0.81, 0.15}, {0.93, 0.24}, {0.12, 0.41},
            {0.23, 0.57}, {0.77, 0.47}, {0.89, 0.60},
            {0.06, 0.71}, {0.32, 0.83}, {0.66, 0.78},
            {0.86, 0.87}, {0.48, 0.91}, {0.73, 0.68},
            {0.94, 0.76}, {0.41, 0.38}, {0.59, 0.55}
        };

        gc.setFill(Color.rgb(180, 211, 243, 0.42));

        for (double[] star : stars)
        {
            double x = star[0] * width;
            double y = star[1] * height;

            gc.fillOval(x, y, 1.7, 1.7);
        }
    }
}
