import javafx.animation.AnimationTimer;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * ForgeOrbit V5 mission dashboard.
 *
 * Active spacecraft states come from ForgeOrbitTimeStepEngine.step(...), not
 * from static transfer estimates. The planner reference panel deliberately
 * remains separate from the live state telemetry.
 */
public class MissionDashboard extends BorderPane
{
    private enum DashboardMode
    {
        EARTH_ORBIT("Earth Orbit"),
        HOHMANN_TRANSFER("Hohmann Transfer"),
        LUNAR_TRANSFER("Lunar Mission"),
        INTERPLANETARY_TRANSFER("Interplanetary Mission");

        private final String label;
        DashboardMode(String label) { this.label = label; }
        public String toString() { return label; }
    }

    private final InputValidator validator = new InputValidator();

    private final TextField missionNameField = new TextField("LEO Raising Mission");
    private final TextField satelliteNameField = new TextField("ForgeSat-1");
    private final TextField dryMassField = new TextField("400");
    private final TextField propellantMassField = new TextField("100");
    private final TextField initialAltitudeField = new TextField("400000");
    private final TextField targetAltitudeField = new TextField("800000");
    private final TextField specificImpulseField = new TextField("320");
    private final TextField thrustField = new TextField("5000");
    private final TextField efficiencyField = new TextField("0.95");

    private final ComboBox<DashboardMode> modeBox = new ComboBox<DashboardMode>();
    private final ComboBox<String> originBox = new ComboBox<String>();
    private final ComboBox<String> destinationBox = new ComboBox<String>();
    private final DatePicker departureDatePicker = new DatePicker(LocalDate.now());
    private final ComboBox<Double> speedBox = new ComboBox<Double>();
    private final ComboBox<Double> timeStepBox = new ComboBox<Double>();

    private final Button initializeButton = new Button("INITIALIZE MISSION");
    private final Button runButton = new Button("RUN");
    private final Button pauseButton = new Button("PAUSE");
    private final Button resetButton = new Button("RESET");
    private final Button simulationPageButton = new Button("SIMULATION");
    private final Button analysisPageButton = new Button("MISSION ANALYSIS");

    private final StackPane pageStack = new StackPane();
    private HBox simulationPage;
    private ScrollPane analysisPage;
    private Label initialAltitudeCaption;
    private Label targetAltitudeCaption;

    private final Label plannedRuntimeValue = new Label("--");
    private final Label analysisVerdictValue = new Label("Initialize a mission to run mathematical checks.");
    private final Label analysisModeValue = new Label("--");
    private final Label analysisRequiredDeltaVValue = new Label("--");
    private final Label analysisAvailableDeltaVValue = new Label("--");
    private final Label analysisDeltaVMarginValue = new Label("--");
    private final Label analysisRequiredPropellantValue = new Label("--");
    private final Label analysisProvidedPropellantValue = new Label("--");
    private final Label analysisTransferTimeValue = new Label("--");
    private final Label analysisRunWindowValue = new Label("--");
    private final Label analysisLaunchWindowValue = new Label("--");
    private final Label analysisAssumptionsValue = new Label("--");

    private final Canvas trajectoryCanvas = new Canvas(720, 560);
    private final StackPane canvasHolder = new StackPane(trajectoryCanvas);

    private final Label topStatus = new Label("AWAITING MISSION CONFIGURATION");
    private final Label modeDescription = new Label("Select a mode and initialize a mission.");
    private final Label phaseValue = new Label("--");
    private final Label elapsedValue = new Label("--");
    private final Label stepCountValue = new Label("--");
    private final Label positionValue = new Label("--");
    private final Label velocityValue = new Label("--");
    private final Label speedValue = new Label("--");
    private final Label accelerationValue = new Label("--");
    private final Label radiusValue = new Label("--");
    private final Label altitudeValue = new Label("--");
    private final Label targetDistanceValue = new Label("--");
    private final Label relativeSpeedValue = new Label("--");
    private final Label energyValue = new Label("--");
    private final Label periodValue = new Label("--");
    private final Label lastDeltaVValue = new Label("--");

    private final Label referenceBurn1Value = new Label("--");
    private final Label referenceBurn2Value = new Label("--");
    private final Label referenceTimeValue = new Label("--");
    private final Label propellantRequiredValue = new Label("--");
    private final Label availableDeltaVValue = new Label("--");
    private final Label marginValue = new Label("--");
    private final Label verdictValue = new Label("Initialize a mission to calculate reference feasibility.");
    private final ListView<String> eventList = new ListView<String>();

    private Mission currentMission;
    private TransferResult currentTransferResult;
    private LunarMissionResult currentLunarPlan;
    private InterplanetaryTransferResult currentInterplanetaryPlan;
    private ForgeOrbitTimeStepEngine activeEngine;
    private DashboardMode activeMode;
    private boolean simulationPlaying;
    private long lastFrameNanos;
    private int lastEventCount = -1;
    private double lastSimulationSpeed = 100000.0;
    private double plannedRuntimeSeconds = Double.NaN;
    private String plannedRuntimeDescription = "--";
    private boolean runtimeWindowCompleted;
    private boolean orbitDemoRuntimePrepared;

    private final AnimationTimer animationTimer = new AnimationTimer()
    {
        @Override
        public void handle(long now)
        {
            if (lastFrameNanos == 0L)
            {
                lastFrameNanos = now;
                refreshDisplay();
                return;
            }
            double wallSeconds = (now - lastFrameNanos) / 1.0e9;
            lastFrameNanos = now;
            if (!Double.isFinite(wallSeconds) || wallSeconds < 0.0) wallSeconds = 0.0;
            wallSeconds = Math.min(wallSeconds, 0.10);

            if (simulationPlaying && activeEngine != null && wallSeconds > 0.0)
            {
                try
                {
                    double remainingWindow = plannedRuntimeSeconds - activeEngine.getElapsedSeconds();
                    if (Double.isFinite(plannedRuntimeSeconds) && remainingWindow <= 1.0e-7)
                    {
                        finishPlannedRuntime();
                    }
                    else
                    {
                        double simulatedSeconds = wallSeconds * selectedSpeed();
                        if (Double.isFinite(remainingWindow))
                            simulatedSeconds = Math.min(simulatedSeconds, Math.max(0.0, remainingWindow));
                        if (simulatedSeconds > 0.0)
                            activeEngine.step(simulatedSeconds);

                        // Once a target orbit has actually been established, keep
                        // three complete revolutions in the runtime window. The
                        // spacecraft is then visibly seen orbiting its target rather
                        // than the app stopping on the exact arrival frame.
                        boolean lunarOrPlanetaryCapture = activeEngine.isOrbitCaptured()
                            && (activeMode == DashboardMode.LUNAR_TRANSFER
                                || activeMode == DashboardMode.INTERPLANETARY_TRANSFER);
                        boolean earthOrbitInsertion = activeMode == DashboardMode.HOHMANN_TRANSFER
                            && activeEngine.isTransferComplete();
                        if (!orbitDemoRuntimePrepared && (lunarOrPlanetaryCapture || earthOrbitInsertion))
                        {
                            orbitDemoRuntimePrepared = true;
                            double orbitPeriod = lunarOrPlanetaryCapture
                                ? activeEngine.getCaptureOrbitPeriodSeconds()
                                : activeEngine.getOsculatingOrbitalPeriodSeconds();
                            if (Double.isFinite(orbitPeriod) && orbitPeriod > 0.0)
                            {
                                plannedRuntimeSeconds = activeEngine.getElapsedSeconds() + 3.0 * orbitPeriod;
                                plannedRuntimeValue.setText(formatDuration(plannedRuntimeSeconds));
                            }
                            if (selectedSpeed() >= 10000.0 && Double.isFinite(orbitPeriod) && orbitPeriod > 0.0)
                            {
                                double demoSpeed = recommendedOrbitDemoSpeed(orbitPeriod);
                                speedBox.setValue(demoSpeed);
                                lastSimulationSpeed = demoSpeed;
                                eventList.getItems().add("ORBIT INSERTION CONFIRMED — playback reduced to "
                                    + formatSpeedScale(demoSpeed) + " to visibly demonstrate three target orbits.");
                            }
                            else
                            {
                                eventList.getItems().add("ORBIT INSERTION CONFIRMED — three target-orbit revolutions are scheduled at the selected playback speed.");
                            }
                        }

                        if (activeEngine.isCollisionDetected())
                        {
                            simulationPlaying = false;
                            activeEngine.pause();
                            setStatus("IMPACT CONDITION DETECTED", "#FF8A8A");
                        }
                        else if (Double.isFinite(plannedRuntimeSeconds)
                                && activeEngine.getElapsedSeconds() >= plannedRuntimeSeconds - 1.0e-6)
                        {
                            finishPlannedRuntime();
                        }
                    }
                }
                catch (RuntimeException ex)
                {
                    simulationPlaying = false;
                    if (activeEngine != null) activeEngine.pause();
                    setStatus("SIMULATION PAUSED — NUMERICAL ERROR", "#FF8A8A");
                    showError("The simulation stopped safely: " + ex.getMessage());
                }
            }
            refreshDisplay();
        }
    };

    public MissionDashboard()
    {
        setPadding(new Insets(14));
        setStyle("-fx-background-color: #07111F;");

        configureInputs();
        simulationPage = createWorkspace();
        analysisPage = createAnalysisPage();
        analysisPage.setVisible(false);
        analysisPage.setManaged(false);
        pageStack.getChildren().addAll(simulationPage, analysisPage);
        setTop(createHeader());
        setCenter(pageStack);
        setBottom(createPlaybackPanel());
        BorderPane.setMargin(getCenter(), new Insets(12, 0, 8, 0));

        initializeButton.setOnAction(event -> initializeMission());
        runButton.setOnAction(event -> runSimulation());
        pauseButton.setOnAction(event -> pauseSimulation());
        resetButton.setOnAction(event -> resetSimulation());
        simulationPageButton.setOnAction(event -> showPage(false));
        analysisPageButton.setOnAction(event -> showPage(true));

        modeBox.valueProperty().addListener((obs, oldMode, newMode) ->
        {
            if (newMode != null && newMode != oldMode) modeChanged(newMode);
        });

        originBox.valueProperty().addListener((obs, oldValue, newValue) ->
        {
            if (modeBox.getValue() == DashboardMode.INTERPLANETARY_TRANSFER && newValue != null)
                applyRecommendedValues(DashboardMode.INTERPLANETARY_TRANSFER, true);
        });
        destinationBox.valueProperty().addListener((obs, oldValue, newValue) ->
        {
            if (modeBox.getValue() == DashboardMode.INTERPLANETARY_TRANSFER && newValue != null)
                applyRecommendedValues(DashboardMode.INTERPLANETARY_TRANSFER, true);
        });

        departureDatePicker.valueProperty().addListener((obs, oldValue, newValue) ->
        {
            if (modeBox.getValue() == DashboardMode.INTERPLANETARY_TRANSFER && newValue != null)
                applyRecommendedValues(DashboardMode.INTERPLANETARY_TRANSFER, false);
        });

        timeStepBox.valueProperty().addListener((obs, oldValue, newValue) ->
        {
            if (activeEngine != null && newValue != null)
            {
                try { activeEngine.setIntegrationStepSeconds(newValue.doubleValue()); }
                catch (IllegalArgumentException ex) { showError(ex.getMessage()); }
                synchronizeEvents();
            }
        });

        speedBox.valueProperty().addListener((obs, oldValue, newValue) ->
        {
            if (newValue != null) lastSimulationSpeed = newValue.doubleValue();
        });

        trajectoryCanvas.widthProperty().bind(canvasHolder.widthProperty());
        trajectoryCanvas.heightProperty().bind(canvasHolder.heightProperty());
        trajectoryCanvas.widthProperty().addListener((obs, oldValue, newValue) -> drawTrajectory());
        trajectoryCanvas.heightProperty().addListener((obs, oldValue, newValue) -> drawTrajectory());

        modeBox.setValue(DashboardMode.EARTH_ORBIT);
        updateModeControls(DashboardMode.EARTH_ORBIT);
        setupSpeedChoices(DashboardMode.EARTH_ORBIT);
        applyRecommendedValues(DashboardMode.EARTH_ORBIT, true);
        updateNavigationStyle(false);
        setButtonsInitialized(false);
        setStatus("AWAITING MISSION CONFIGURATION", "#8EA7C5");
        drawTrajectory();
        animationTimer.start();
    }

    private void configureInputs()
    {
        modeBox.setItems(FXCollections.observableArrayList(DashboardMode.values()));
        modeBox.setMaxWidth(Double.MAX_VALUE);
        originBox.setItems(FXCollections.observableArrayList(
            "Mercury", "Venus", "Earth", "Mars", "Jupiter", "Saturn", "Uranus", "Neptune"));
        destinationBox.setItems(FXCollections.observableArrayList(
            "Mercury", "Venus", "Earth", "Mars", "Jupiter", "Saturn", "Uranus", "Neptune"));
        originBox.setValue("Earth");
        destinationBox.setValue("Mars");
        originBox.setMaxWidth(Double.MAX_VALUE);
        destinationBox.setMaxWidth(Double.MAX_VALUE);
        departureDatePicker.setMaxWidth(Double.MAX_VALUE);

        for (TextField field : Arrays.asList(missionNameField, satelliteNameField, dryMassField,
                propellantMassField, initialAltitudeField, targetAltitudeField,
                specificImpulseField, thrustField, efficiencyField))
            styleTextField(field);

        styleButton(initializeButton, "#087E8B", "#FFFFFF");
        styleButton(runButton, "#12644D", "#83F0BB");
        styleButton(pauseButton, "#73551B", "#FFDA7B");
        styleButton(resetButton, "#203A57", "#B7D7F6");

        timeStepBox.setMaxWidth(Double.MAX_VALUE);
        timeStepBox.setConverter(new StringConverter<Double>()
        {
            @Override public String toString(Double seconds)
            { return seconds == null ? "--" : formatNumber(seconds) + " s"; }
            @Override public Double fromString(String value) { return Double.valueOf(value.replace(" s", "")); }
        });
        speedBox.setMaxWidth(Double.MAX_VALUE);
        speedBox.setConverter(new StringConverter<Double>()
        {
            @Override public String toString(Double scale) { return scale == null ? "--" : formatSpeedScale(scale); }
            @Override public Double fromString(String value) { return parseSpeedScale(value); }
        });
        setTimeStepChoices(DashboardMode.EARTH_ORBIT);
    }

    private VBox createHeader()
    {
        Label title = new Label("FORGEORBIT");
        title.setStyle("-fx-font-size: 27px; -fx-font-weight: bold; -fx-text-fill: #F0F6FF;");
        Label subtitle = new Label("V5  /  TIME-STEPPED ORBITAL SIMULATION & MISSION TELEMETRY");
        subtitle.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #7091B8;");
        VBox titleBox = new VBox(5, title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label modeLabel = smallHeading("ACTIVE SIMULATION MODE");
        VBox modeBoxWrap = new VBox(5, modeLabel, modeBox);
        modeBoxWrap.setMinWidth(230);
        Label version = new Label("V5");
        version.setStyle("-fx-background-color: #123B4D; -fx-text-fill: #69D8F2; -fx-padding: 7 16 7 16; -fx-background-radius: 7; -fx-font-weight: bold;");

        styleButton(simulationPageButton, "#123B4D", "#69D8F2");
        styleButton(analysisPageButton, "#203A57", "#B7D7F6");
        HBox pageNavigation = new HBox(6, simulationPageButton, analysisPageButton);
        pageNavigation.setAlignment(Pos.CENTER_LEFT);
        HBox row = new HBox(12, titleBox, spacer, pageNavigation, modeBoxWrap, version);
        row.setAlignment(Pos.CENTER_LEFT);
        VBox header = new VBox(10, row, separator());
        header.setPadding(new Insets(3, 2, 8, 2));
        return header;
    }

    private HBox createWorkspace()
    {
        ScrollPane inputPane = createInputPanel();
        VBox simulationPane = createSimulationPanel();
        ScrollPane telemetryPane = createTelemetryPanel();

        inputPane.setPrefWidth(270);
        inputPane.setMinWidth(250);
        telemetryPane.setPrefWidth(320);
        telemetryPane.setMinWidth(300);
        HBox.setHgrow(simulationPane, Priority.ALWAYS);
        simulationPane.setMinWidth(390);

        HBox workspace = new HBox(10, inputPane, simulationPane, telemetryPane);
        workspace.setAlignment(Pos.TOP_CENTER);
        return workspace;
    }

    private ScrollPane createInputPanel()
    {
        VBox form = panelBox(12);
        form.setPadding(new Insets(15));
        form.setPrefWidth(270);

        Label note = new Label("Recommended values update when you change simulation mode or planet pair. SI units are used. The propagator models coast arcs and impulsive burns; the analysis page states each model's assumptions.");
        note.setWrapText(true);
        note.setStyle("-fx-text-fill: #7791AF; -fx-font-size: 10px;");

        VBox modeSettings = new VBox(8,
            fieldGroup("Origin planet (interplanetary)", originBox),
            fieldGroup("Destination planet", destinationBox),
            fieldGroup("Departure date", departureDatePicker));

        form.getChildren().addAll(
            sectionHeading("MISSION CONFIGURATION"),
            smallHeading("IDENTIFICATION"),
            fieldGroup("Mission name", missionNameField),
            fieldGroup("Spacecraft name", satelliteNameField),
            separator(),
            smallHeading("SPACECRAFT MASS"),
            fieldGroup("Dry mass (kg)", dryMassField),
            fieldGroup("Propellant mass (kg)", propellantMassField),
            separator(),
            smallHeading("EARTH ORBIT / LUNAR PARAMETERS"),
            fieldGroup("Initial / parking altitude (m)", initialAltitudeField),
            fieldGroup("Target orbit altitude (m)", targetAltitudeField),
            separator(),
            smallHeading("PROPULSION REFERENCE"),
            fieldGroup("Specific impulse (s)", specificImpulseField),
            fieldGroup("Engine thrust (N)", thrustField),
            fieldGroup("Efficiency (0–1)", efficiencyField),
            separator(),
            smallHeading("PLANETARY TRANSFER SETTINGS"),
            modeSettings,
            initializeButton,
            note);

        ScrollPane pane = new ScrollPane(form);
        pane.setFitToWidth(true);
        pane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        pane.setStyle("-fx-background: #0C1A2B; -fx-background-color: #0C1A2B; -fx-border-color: transparent;");
        return pane;
    }

    private VBox createSimulationPanel()
    {
        VBox panel = panelBox(10);
        panel.setPadding(new Insets(14));

        Label heading = sectionHeading("LIVE TRAJECTORY VIEW");
        modeDescription.setStyle("-fx-text-fill: #7894B5; -fx-font-size: 11px;");
        Label plannedLegend = legend("PLANNED PATH", "#36BFFA", true);
        Label actualLegend = legend("ACTUAL PATH", "#35D6A3", false);
        Label targetLegend = legend("TARGET BODY", "#FFB454", false);
        HBox legends = new HBox(13, plannedLegend, actualLegend, targetLegend);
        legends.setAlignment(Pos.CENTER_LEFT);

        canvasHolder.setMinSize(350, 370);
        canvasHolder.setStyle("-fx-background-color: #07111F; -fx-background-radius: 12; -fx-border-color: #203A57; -fx-border-radius: 12;");
        VBox.setVgrow(canvasHolder, Priority.ALWAYS);

        VBox statusBox = panelBox(5);
        statusBox.setPadding(new Insets(10, 12, 10, 12));
        Label statusHeading = smallHeading("SIMULATION STATUS");
        topStatus.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #8EA7C5;");
        phaseValue.setStyle("-fx-font-size: 11px; -fx-text-fill: #D7E8FA;");
        statusBox.getChildren().addAll(statusHeading, topStatus, phaseValue);

        Label footer = new Label("2-D X/Y projection  •  Z is retained in the numerical state  •  body markers are not to scale");
        footer.setWrapText(true);
        footer.setStyle("-fx-font-size: 9px; -fx-text-fill: #6785A4;");
        panel.getChildren().addAll(heading, modeDescription, legends, canvasHolder, statusBox, footer);
        VBox.setVgrow(panel, Priority.ALWAYS);
        return panel;
    }

    private ScrollPane createTelemetryPanel()
    {
        VBox panel = panelBox(10);
        panel.setPadding(new Insets(14));
        panel.setPrefWidth(320);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(7);
        grid.add(telemetryName("Elapsed time"), 0, 0); grid.add(telemetryValue(elapsedValue), 1, 0);
        grid.add(telemetryName("Integration steps"), 0, 1); grid.add(telemetryValue(stepCountValue), 1, 1);
        grid.add(telemetryName("Position X / Y / Z"), 0, 2); grid.add(telemetryValue(positionValue), 1, 2);
        grid.add(telemetryName("Velocity vector"), 0, 3); grid.add(telemetryValue(velocityValue), 1, 3);
        grid.add(telemetryName("Speed"), 0, 4); grid.add(telemetryValue(speedValue), 1, 4);
        grid.add(telemetryName("Acceleration"), 0, 5); grid.add(telemetryValue(accelerationValue), 1, 5);
        grid.add(telemetryName("Primary radius"), 0, 6); grid.add(telemetryValue(radiusValue), 1, 6);
        grid.add(telemetryName("Primary altitude"), 0, 7); grid.add(telemetryValue(altitudeValue), 1, 7);
        grid.add(telemetryName("Target distance"), 0, 8); grid.add(telemetryValue(targetDistanceValue), 1, 8);
        grid.add(telemetryName("Relative target speed"), 0, 9); grid.add(telemetryValue(relativeSpeedValue), 1, 9);
        grid.add(telemetryName("Specific energy"), 0, 10); grid.add(telemetryValue(energyValue), 1, 10);
        grid.add(telemetryName("Osculating period"), 0, 11); grid.add(telemetryValue(periodValue), 1, 11);
        grid.add(telemetryName("Last impulsive Δv"), 0, 12); grid.add(telemetryValue(lastDeltaVValue), 1, 12);
        grid.add(telemetryName("Planned run window"), 0, 13); grid.add(telemetryValue(plannedRuntimeValue), 1, 13);

        Label referenceHeading = sectionHeading("PLANNING REFERENCE");
        Label referenceNote = new Label("These are mode-specific planning values derived from the selected ideal transfer model and rocket equation. They are separate from the live time-stepped telemetry.");
        referenceNote.setWrapText(true);
        referenceNote.setStyle("-fx-text-fill: #7894B5; -fx-font-size: 9px;");
        VBox referenceCard = panelBox(7);
        referenceCard.setPadding(new Insets(10));
        referenceCard.getChildren().addAll(
            resultRow("Reference first burn", referenceBurn1Value),
            resultRow("Reference second burn", referenceBurn2Value),
            resultRow("Reference transfer time", referenceTimeValue),
            resultRow("Propellant required", propellantRequiredValue),
            resultRow("Available Δv", availableDeltaVValue),
            resultRow("Δv margin", marginValue),
            verdictValue);
        verdictValue.setWrapText(true);
        verdictValue.setStyle("-fx-text-fill: #83F0BB; -fx-font-size: 10px; -fx-font-weight: bold;");

        Label integrationHeading = sectionHeading("INTEGRATOR SETTINGS");
        VBox integrationBox = new VBox(5,
            fieldGroup("RK4 integration step", timeStepBox),
            fieldGroup("Simulation time scale", speedBox));

        Label eventHeading = sectionHeading("MISSION EVENT LOG");
        eventList.setPrefHeight(150);
        eventList.setStyle("-fx-control-inner-background: #07111F; -fx-background-color: #07111F; -fx-text-fill: #A9BED5; -fx-border-color: #203750;");

        panel.getChildren().addAll(
            sectionHeading("LIVE TELEMETRY"),
            grid,
            separator(),
            referenceHeading,
            referenceNote,
            referenceCard,
            separator(),
            integrationHeading,
            integrationBox,
            separator(),
            eventHeading,
            eventList);

        ScrollPane pane = new ScrollPane(panel);
        pane.setFitToWidth(true);
        pane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        pane.setStyle("-fx-background: #0C1A2B; -fx-background-color: #0C1A2B; -fx-border-color: transparent;");
        return pane;
    }

    private ScrollPane createAnalysisPage()
    {
        VBox content = panelBox(16);
        content.setPadding(new Insets(22));
        content.setMaxWidth(Double.MAX_VALUE);

        Label title = new Label("MISSION FEASIBILITY ANALYSIS");
        title.setStyle("-fx-text-fill: #F0F6FF; -fx-font-size: 20px; -fx-font-weight: bold;");
        Label subtitle = new Label("Analytical checks use the selected mode's transfer model, rocket equation and supplied spacecraft parameters. This is a mathematical screening result, not a guarantee of a real flight.");
        subtitle.setWrapText(true);
        subtitle.setStyle("-fx-text-fill: #8EA7C5; -fx-font-size: 11px;");

        analysisVerdictValue.setWrapText(true);
        analysisVerdictValue.setMinHeight(52);
        analysisVerdictValue.setStyle("-fx-text-fill: #69D8F2; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 15; -fx-background-color: #11263A; -fx-background-radius: 8;");
        analysisModeValue.setStyle("-fx-text-fill: #D7E8FA; -fx-font-size: 12px; -fx-font-weight: bold;");

        GridPane metrics = new GridPane();
        metrics.setHgap(20);
        metrics.setVgap(13);
        metrics.add(analysisRow("Selected mission mode", analysisModeValue), 0, 0);
        metrics.add(analysisRow("Required mission Δv", analysisRequiredDeltaVValue), 0, 1);
        metrics.add(analysisRow("Available vehicle Δv", analysisAvailableDeltaVValue), 1, 1);
        metrics.add(analysisRow("Δv margin", analysisDeltaVMarginValue), 0, 2);
        metrics.add(analysisRow("Required propellant", analysisRequiredPropellantValue), 1, 2);
        metrics.add(analysisRow("Configured propellant", analysisProvidedPropellantValue), 0, 3);
        metrics.add(analysisRow("Nominal transfer time", analysisTransferTimeValue), 1, 3);
        metrics.add(analysisRow("Configured simulation window", analysisRunWindowValue), 0, 4);
        metrics.add(analysisRow("Launch-window wait", analysisLaunchWindowValue), 1, 4);
        for (javafx.scene.Node node : metrics.getChildren())
            if (node instanceof VBox) GridPane.setHgrow(node, Priority.ALWAYS);

        Label assumptionsHeading = sectionHeading("MODEL, EQUATIONS & LIMITATIONS");
        analysisAssumptionsValue.setWrapText(true);
        analysisAssumptionsValue.setStyle("-fx-text-fill: #A9BED5; -fx-font-size: 11px; -fx-padding: 12; -fx-background-color: #07111F; -fx-background-radius: 8;");
        Label equation = new Label("Rocket equation: Δv = Isp · g₀ · ln(m₀ / m₁)     |     Feasibility check: available Δv ≥ required Δv and available propellant ≥ required propellant");
        equation.setWrapText(true);
        equation.setStyle("-fx-text-fill: #69D8F2; -fx-font-size: 10px; -fx-font-family: 'Consolas';");
        Label caveat = new Label("Important: the current timestep engine propagates impulsive/coast trajectories. It does not yet consume propellant over finite-duration thrust burns, model full 3-D planetary perturbations, or guarantee lunar/interplanetary rendezvous. For those modes the result answers whether the idealised planning Δv is compatible with the configured propulsion—not whether the simulated path has successfully arrived.");
        caveat.setWrapText(true);
        caveat.setStyle("-fx-text-fill: #FFDA7B; -fx-font-size: 10px;");

        Button refreshAnalysisButton = new Button("REINITIALIZE AND RECHECK FEASIBILITY");
        styleButton(refreshAnalysisButton, "#087E8B", "#FFFFFF");
        refreshAnalysisButton.setOnAction(event -> initializeMission());

        content.getChildren().addAll(title, subtitle, separator(), analysisVerdictValue,
            metrics, separator(), assumptionsHeading, analysisAssumptionsValue, equation, caveat, refreshAnalysisButton);
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: #07111F; -fx-background-color: #07111F; -fx-border-color: transparent;");
        return scroll;
    }

    private VBox analysisRow(String title, Label value)
    {
        Label caption = new Label(title);
        caption.setStyle("-fx-text-fill: #7894B5; -fx-font-size: 10px; -fx-font-weight: bold;");
        value.setWrapText(true);
        value.setStyle("-fx-text-fill: #E5F4FF; -fx-font-size: 13px; -fx-font-weight: bold;");
        VBox card = new VBox(7, caption, value);
        card.setPadding(new Insets(12));
        card.setMinWidth(240);
        card.setStyle("-fx-background-color: #11263A; -fx-background-radius: 8; -fx-border-color: #203D59; -fx-border-radius: 8;");
        return card;
    }

    private void showPage(boolean analysis)
    {
        if (simulationPage != null)
        {
            simulationPage.setVisible(!analysis);
            simulationPage.setManaged(!analysis);
        }
        if (analysisPage != null)
        {
            analysisPage.setVisible(analysis);
            analysisPage.setManaged(analysis);
        }
        updateNavigationStyle(analysis);
        if (analysis) refreshAnalysisPage();
    }

    private void updateNavigationStyle(boolean analysis)
    {
        simulationPageButton.setStyle(analysis
            ? "-fx-background-color: #203A57; -fx-text-fill: #B7D7F6; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 9 12 9 12; -fx-background-radius: 6;"
            : "-fx-background-color: #087E8B; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 9 12 9 12; -fx-background-radius: 6;");
        analysisPageButton.setStyle(analysis
            ? "-fx-background-color: #087E8B; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 9 12 9 12; -fx-background-radius: 6;"
            : "-fx-background-color: #203A57; -fx-text-fill: #B7D7F6; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 9 12 9 12; -fx-background-radius: 6;");
    }

    private void refreshAnalysisPage()
    {
        if (activeMode == null || currentMission == null)
        {
            analysisModeValue.setText("No mission initialized");
            analysisRequiredDeltaVValue.setText("--");
            analysisAvailableDeltaVValue.setText("--");
            analysisDeltaVMarginValue.setText("--");
            analysisRequiredPropellantValue.setText("--");
            analysisProvidedPropellantValue.setText("--");
            analysisTransferTimeValue.setText("--");
            analysisRunWindowValue.setText("--");
            analysisLaunchWindowValue.setText("--");
            analysisAssumptionsValue.setText("Initialize a mission to calculate mode-specific feasibility.");
            analysisVerdictValue.setText("NO RESULT — INITIALIZE A MISSION FIRST");
            analysisVerdictValue.setStyle("-fx-text-fill: #69D8F2; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 15; -fx-background-color: #11263A; -fx-background-radius: 8;");
            return;
        }

        analysisModeValue.setText(activeMode.toString());
        analysisRequiredDeltaVValue.setText(format(planRequiredDeltaV, "m/s"));
        analysisAvailableDeltaVValue.setText(format(planAvailableDeltaV, "m/s"));
        analysisDeltaVMarginValue.setText(format(planDeltaVMargin, "m/s"));
        analysisRequiredPropellantValue.setText(format(planRequiredPropellant, "kg"));
        analysisProvidedPropellantValue.setText(format(currentMission.getSatellite().getPropellantMass(), "kg"));
        analysisTransferTimeValue.setText(formatDuration(planTransferSeconds));
        analysisRunWindowValue.setText(formatDuration(plannedRuntimeSeconds) + " (" + plannedRuntimeDescription + ")");
        analysisLaunchWindowValue.setText(activeMode == DashboardMode.INTERPLANETARY_TRANSFER
            ? formatDuration(planLaunchWaitSeconds) + " until recommended departure" : "N/A for this mode");
        analysisAssumptionsValue.setText(planAssumptions);

        if (planFeasible)
        {
            analysisVerdictValue.setText("MATHEMATICALLY FEASIBLE UNDER THE SELECTED IDEAL MODEL");
            analysisVerdictValue.setStyle("-fx-text-fill: #83F0BB; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 15; -fx-background-color: #11263A; -fx-background-radius: 8;");
        }
        else
        {
            analysisVerdictValue.setText("NOT FEASIBLE WITH THE CURRENT PROPULSION / PROPELLANT SETTINGS");
            analysisVerdictValue.setStyle("-fx-text-fill: #FF8A8A; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 15; -fx-background-color: #11263A; -fx-background-radius: 8;");
        }
    }

    private HBox createPlaybackPanel()
    {
        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setPadding(new Insets(12, 14, 12, 14));
        controls.setStyle("-fx-background-color: #0C1A2B; -fx-background-radius: 10; -fx-border-color: #1B3552; -fx-border-radius: 10;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label timeScaleLabel = new Label("SIMULATION TIME IS ADVANCED BY RK4 TIMESTEPS");
        timeScaleLabel.setStyle("-fx-text-fill: #587695; -fx-font-size: 9px; -fx-font-weight: bold;");
        controls.getChildren().addAll(runButton, pauseButton, resetButton, spacer, timeScaleLabel);
        return controls;
    }

    private void initializeMission()
    {
        if (activeEngine != null) activeEngine.stop();
        simulationPlaying = false;
        runtimeWindowCompleted = false;
        orbitDemoRuntimePrepared = false;
        try
        {
            String missionName = missionNameField.getText().trim();
            String satelliteName = satelliteNameField.getText().trim();
            double dryMass = readNumber(dryMassField, "Dry mass");
            double propellantMass = readNumber(propellantMassField, "Propellant mass");
            double initialAltitude = readNumber(initialAltitudeField, "Initial altitude");
            double targetAltitude = readNumber(targetAltitudeField, "Target altitude");
            double specificImpulse = readNumber(specificImpulseField, "Specific impulse");
            double thrust = readNumber(thrustField, "Thrust");
            double efficiency = readNumber(efficiencyField, "Efficiency");

            if (!validator.isValidMissionName(missionName)) throw new IllegalArgumentException("Enter a valid mission name.");
            if (!validator.isValidMissionName(satelliteName)) throw new IllegalArgumentException("Enter a valid spacecraft name.");
            if (!validator.isValidMass(dryMass)) throw new IllegalArgumentException("Dry mass must be finite and positive.");
            if (!validator.isValidPropellantMass(propellantMass)) throw new IllegalArgumentException("Propellant mass must be finite and non-negative.");
            if (!validator.isValidAltitude(initialAltitude)) throw new IllegalArgumentException("Initial/parking altitude must be positive.");
            if (!validator.isValidAltitude(targetAltitude)) throw new IllegalArgumentException("Target altitude must be positive.");
            if (modeBox.getValue() == DashboardMode.HOHMANN_TRANSFER
                    && Math.abs(initialAltitude - targetAltitude) < 1.0e-6)
                throw new IllegalArgumentException("Initial and target orbit altitudes must differ for a Hohmann transfer.");
            if (!validator.isValidSpecificImpulse(specificImpulse)) throw new IllegalArgumentException("Specific impulse must be positive.");
            if (!validator.isValidThrust(thrust)) throw new IllegalArgumentException("Thrust must be positive.");
            if (!validator.isValidEfficiency(efficiency)) throw new IllegalArgumentException("Efficiency must be greater than zero and at most one.");
            if (departureDatePicker.getValue() == null) throw new IllegalArgumentException("Choose a departure date.");

            Orbit initialOrbit = new Orbit(initialAltitude);
            Orbit targetOrbit = new Orbit(targetAltitude);
            Satellite satellite = new Satellite(satelliteName, dryMass, propellantMass, initialOrbit, targetOrbit);
            Propulsion propulsion = new Propulsion("Mission Engine", specificImpulse, thrust, efficiency);
            currentMission = new Mission(missionName, satellite, propulsion);
            currentTransferResult = currentMission.calculateTransfer();
            DashboardMode selectedMode = modeBox.getValue();
            currentLunarPlan = null;
            currentInterplanetaryPlan = null;
            if (selectedMode == DashboardMode.LUNAR_TRANSFER)
            {
                if (initialAltitude < 100000.0)
                    throw new IllegalArgumentException("Lunar missions require a parking orbit of at least 100 km in this model.");
                currentLunarPlan = new LunarMissionCalculator().calculateTransfer(initialAltitude, targetAltitude / 1000.0);
            }
            if (selectedMode == DashboardMode.INTERPLANETARY_TRANSFER)
            {
                String origin = originBox.getValue();
                String destination = destinationBox.getValue();
                if (origin == null || destination == null || origin.equalsIgnoreCase(destination))
                    throw new IllegalArgumentException("Choose two different origin and destination planets.");
                currentInterplanetaryPlan = new InterplanetaryTransfer().calculateTransfer(
                    origin, destination, departureDatePicker.getValue().atTime(12, 0));
            }
            activeEngine = createEngine(selectedMode, initialAltitude, targetAltitude);
            updateReferenceAnalysis(currentMission, currentTransferResult, initialOrbit, targetOrbit);
            plannedRuntimeSeconds = calculatePlannedRuntime(selectedMode, initialAltitude, targetAltitude);
            plannedRuntimeDescription = describeRuntime(selectedMode);
            plannedRuntimeValue.setText(formatDuration(plannedRuntimeSeconds));
            runtimeWindowCompleted = false;
            Double integrationStep = timeStepBox.getValue();
            if (integrationStep != null) activeEngine.setIntegrationStepSeconds(integrationStep.doubleValue());
            activeEngine.pause();
            activeMode = selectedMode;
            simulationPlaying = false;
            lastFrameNanos = 0L;
            lastEventCount = -1;
            synchronizeEvents();
            setButtonsInitialized(true);
            refreshAnalysisPage();
            refreshDisplay();
            setStatus("MISSION INITIALIZED — PRESS RUN", "#69D8F2");
        }
        catch (NumberFormatException ex)
        {
            showError(ex.getMessage() == null ? "Enter a valid number in every numerical field." : ex.getMessage());
        }
        catch (IllegalArgumentException ex)
        {
            showError(ex.getMessage());
        }
        catch (RuntimeException ex)
        {
            showError("Mission initialization failed: " + ex.getMessage());
        }
    }

    private ForgeOrbitTimeStepEngine createEngine(DashboardMode mode, double initialAltitude, double targetAltitude)
    {
        if (mode == DashboardMode.EARTH_ORBIT)
            return ForgeOrbitTimeStepEngine.earthOrbit(initialAltitude);
        if (mode == DashboardMode.HOHMANN_TRANSFER)
            return ForgeOrbitTimeStepEngine.hohmannTransfer(initialAltitude, targetAltitude);
        if (mode == DashboardMode.LUNAR_TRANSFER)
            return ForgeOrbitTimeStepEngine.lunarTransfer(initialAltitude);
        if (mode == DashboardMode.INTERPLANETARY_TRANSFER)
        {
            String origin = originBox.getValue();
            String destination = destinationBox.getValue();
            if (origin == null || destination == null) throw new IllegalArgumentException("Choose origin and destination planets.");
            if (origin.equalsIgnoreCase(destination)) throw new IllegalArgumentException("Origin and destination planets must be different.");
            LocalDate date = departureDatePicker.getValue();
            LocalDateTime requestedDeparture = date.atTime(12, 0);
            if (currentInterplanetaryPlan == null)
                currentInterplanetaryPlan = new InterplanetaryTransfer().calculateTransfer(origin, destination, requestedDeparture);
            LocalDateTime launchDate = currentInterplanetaryPlan.getEstimatedDeparture();
            return ForgeOrbitTimeStepEngine.interplanetaryTransfer(origin, destination, launchDate);
        }
        throw new IllegalArgumentException("Unsupported simulation mode.");
    }

    private double planRequiredDeltaV;
    private double planRequiredPropellant;
    private double planAvailableDeltaV;
    private double planDeltaVMargin;
    private double planTransferSeconds;
    private double planLaunchWaitSeconds;
    private boolean planFeasible;
    private String planAssumptions = "--";

    private void updateReferenceAnalysis(Mission mission, TransferResult result, Orbit initialOrbit, Orbit targetOrbit)
    {
        DashboardMode mode = modeBox.getValue();
        HohmannTransfer hohmann = new HohmannTransfer();
        PropellantCalculator propellantCalculator = new PropellantCalculator();
        double burn1 = 0.0;
        double burn2 = 0.0;
        double requiredDeltaV = 0.0;
        double timeSeconds = 0.0;
        double launchWait = 0.0;
        String assumptions;

        if (mode == DashboardMode.EARTH_ORBIT)
        {
            double radius = Constants.EARTH_RADIUS + initialOrbit.getAltitude();
            timeSeconds = Constants.TWO_PI * Math.sqrt(radius * radius * radius / Constants.EARTH_MU);
            assumptions = "Ideal circular Earth orbit under a two-body Newtonian gravity model. No orbit-insertion burn is simulated in this mode.";
        }
        else if (mode == DashboardMode.HOHMANN_TRANSFER)
        {
            burn1 = hohmann.calculateFirstBurnDeltaV(initialOrbit, targetOrbit);
            burn2 = hohmann.calculateSecondBurnDeltaV(initialOrbit, targetOrbit);
            requiredDeltaV = burn1 + burn2;
            timeSeconds = hohmann.calculateTransferTime(initialOrbit, targetOrbit);
            assumptions = "Ideal coplanar impulsive Hohmann transfer between circular Earth orbits. Finite-duration burns and perturbations are excluded.";
        }
        else if (mode == DashboardMode.LUNAR_TRANSFER)
        {
            if (currentLunarPlan == null)
                currentLunarPlan = new LunarMissionCalculator().calculateTransfer(initialOrbit.getAltitude(), targetOrbit.getAltitude() / 1000.0);
            burn1 = activeEngine != null ? activeEngine.getInitialDepartureDeltaVMetresPerSecond() : currentLunarPlan.getDepartureDeltaV();
            burn2 = activeEngine != null && Double.isFinite(activeEngine.getPlannedCaptureDeltaVMetresPerSecond())
                ? activeEngine.getPlannedCaptureDeltaVMetresPerSecond() : currentLunarPlan.getLunarCaptureDeltaV();
            requiredDeltaV = burn1 + burn2;
            timeSeconds = activeEngine != null && Double.isFinite(activeEngine.getPlannedTransferSeconds())
                ? activeEngine.getPlannedTransferSeconds() : currentLunarPlan.getTransferTimeSeconds();
            assumptions = "Time-stepped Earth–Moon transfer with a planned circular-orbit insertion at 100 km lunar altitude. Required capture Δv is estimated by a dry-run of the same RK4 propagator. Simplified circular lunar ephemeris; impulsive burns, no spacecraft mass depletion during the coast.";
        }
        else
        {
            if (currentInterplanetaryPlan == null)
                currentInterplanetaryPlan = new InterplanetaryTransfer().calculateTransfer(
                    originBox.getValue(), destinationBox.getValue(), departureDatePicker.getValue().atTime(12, 0));
            burn1 = activeEngine != null ? activeEngine.getInitialDepartureDeltaVMetresPerSecond() : currentInterplanetaryPlan.getDepartureDeltaV();
            burn2 = activeEngine != null && Double.isFinite(activeEngine.getPlannedCaptureDeltaVMetresPerSecond())
                ? activeEngine.getPlannedCaptureDeltaVMetresPerSecond() : currentInterplanetaryPlan.getArrivalDeltaV();
            requiredDeltaV = burn1 + burn2;
            timeSeconds = activeEngine != null && Double.isFinite(activeEngine.getPlannedTransferSeconds())
                ? activeEngine.getPlannedTransferSeconds() : currentInterplanetaryPlan.getTransferTimeSeconds();
            launchWait = currentInterplanetaryPlan.getLaunchWaitSeconds();
            assumptions = "Lambert-targeted solar-gravity coast, followed by an impulsive circular-orbit insertion around the selected destination. Arrival/capture burn is estimated with the same RK4 propagation. Approximate Keplerian planetary ephemeris; planetary perturbations and finite-thrust dynamics are omitted.";
        }

        double requiredPropellant = propellantCalculator.calculateRequiredPropellant(
            requiredDeltaV, mission.getSatellite().getDryMass(), mission.getPropulsion());
        double availableDeltaV = propellantCalculator.calculateAvailableDeltaV(
            mission.getSatellite().getDryMass(), mission.getSatellite().getPropellantMass(), mission.getPropulsion());
        double margin = availableDeltaV - requiredDeltaV;
        boolean feasible = availableDeltaV + 1.0e-9 >= requiredDeltaV
            && requiredPropellant <= mission.getSatellite().getPropellantMass() + 1.0e-9;

        planRequiredDeltaV = requiredDeltaV;
        planRequiredPropellant = requiredPropellant;
        planAvailableDeltaV = availableDeltaV;
        planDeltaVMargin = margin;
        planTransferSeconds = timeSeconds;
        planLaunchWaitSeconds = launchWait;
        planFeasible = feasible;
        planAssumptions = assumptions;

        referenceBurn1Value.setText(format(burn1, "m/s"));
        referenceBurn2Value.setText(format(burn2, "m/s"));
        referenceTimeValue.setText(formatDuration(timeSeconds));
        propellantRequiredValue.setText(format(requiredPropellant, "kg"));
        availableDeltaVValue.setText(format(availableDeltaV, "m/s"));
        marginValue.setText(format(margin, "m/s"));
        verdictValue.setText(feasible
            ? "MATHEMATICALLY FEASIBLE under the stated ideal-model assumptions."
            : "NOT FEASIBLE with the current propellant / propulsion reference values.");
        verdictValue.setStyle(feasible
            ? "-fx-text-fill: #83F0BB; -fx-font-size: 10px; -fx-font-weight: bold;"
            : "-fx-text-fill: #FF8A8A; -fx-font-size: 10px; -fx-font-weight: bold;");
    }

    private void applyRecommendedValues(DashboardMode mode, boolean updateMissionName)
    {
        double dryMass = 400.0;
        double initialAltitude = 400000.0;
        double targetAltitude = 800000.0;
        double isp = 320.0;
        double thrust = 5000.0;
        double efficiency = 0.95;
        double recommendedDeltaV = 0.0;

        if (mode == DashboardMode.HOHMANN_TRANSFER)
        {
            dryMass = 400.0;
            initialAltitude = 200000.0;
            targetAltitude = 35786000.0;
            isp = 450.0;
            thrust = 15000.0;
            efficiency = 0.98;
            recommendedDeltaV = new HohmannTransfer().calculateTransfer(
                new Orbit(initialAltitude), new Orbit(targetAltitude)).getTotalDeltaV();
            if (updateMissionName) missionNameField.setText("LEO-to-GEO Hohmann Transfer");
        }
        else if (mode == DashboardMode.LUNAR_TRANSFER)
        {
            dryMass = 400.0;
            initialAltitude = 200000.0;
            targetAltitude = 100000.0;
            isp = 450.0;
            thrust = 15000.0;
            efficiency = 0.98;
            recommendedDeltaV = new LunarMissionCalculator().calculateTransfer(initialAltitude, targetAltitude / 1000.0).getTotalDeltaV();
            if (updateMissionName) missionNameField.setText("Earth-to-Moon Transfer");
        }
        else if (mode == DashboardMode.INTERPLANETARY_TRANSFER)
        {
            dryMass = 1000.0;
            initialAltitude = 400000.0;
            targetAltitude = 800000.0;
            isp = 450.0;
            thrust = 25000.0;
            efficiency = 0.98;
            String origin = originBox.getValue() == null ? "Earth" : originBox.getValue();
            String destination = destinationBox.getValue() == null ? "Mars" : destinationBox.getValue();
            if (!origin.equalsIgnoreCase(destination))
            {
                LocalDate requestedDate = departureDatePicker.getValue() == null ? LocalDate.now() : departureDatePicker.getValue();
                InterplanetaryTransferResult previewPlan = new InterplanetaryTransfer().calculateTransfer(
                    origin, destination, requestedDate.atTime(12, 0));
                // Base the recommended fuel load on the same propagated route
                // and insertion burn that the simulator will attempt, rather
                // than only on the legacy circular-orbit Hohmann estimate.
                ForgeOrbitTimeStepEngine previewEngine = ForgeOrbitTimeStepEngine.interplanetaryTransfer(
                    origin, destination, previewPlan.getEstimatedDeparture());
                recommendedDeltaV = previewEngine.getPlannedMissionDeltaVMetresPerSecond();
            }
            if (updateMissionName) missionNameField.setText(origin + "-to-" + destination + " Interplanetary Mission");
        }
        else
        {
            if (updateMissionName) missionNameField.setText("LEO Circular Orbit Demonstration");
        }

        dryMassField.setText(formatInput(dryMass));
        initialAltitudeField.setText(formatInput(initialAltitude));
        targetAltitudeField.setText(formatInput(targetAltitude));
        specificImpulseField.setText(formatInput(isp));
        thrustField.setText(formatInput(thrust));
        efficiencyField.setText(formatInput(efficiency));

        // Current reference calculators understate the capture impulse used by
        // the time-stepped model. Add mode-specific conservative multipliers so
        // auto-filled vehicle values do not promise a mission with too little fuel.
        if (mode == DashboardMode.LUNAR_TRANSFER) recommendedDeltaV *= 1.60;
        // Interplanetary recommendations already use the active model's actual
        // departure plus planned insertion delta-v from the engine preflight.

        double recommendedPropellant = mode == DashboardMode.EARTH_ORBIT ? 100.0 : 150.0;
        if (recommendedDeltaV > 0.0)
        {
            Propulsion p = new Propulsion("Recommendation Engine", isp, thrust, efficiency);
            double base = new PropellantCalculator().calculateRequiredPropellant(
                recommendedDeltaV * 1.10, dryMass, p);
            recommendedPropellant = roundUp(base * 1.20, mode == DashboardMode.INTERPLANETARY_TRANSFER ? 100.0 : 50.0);
        }
        propellantMassField.setText(formatInput(recommendedPropellant));
        if (mode == DashboardMode.INTERPLANETARY_TRANSFER)
        {
            initialAltitudeField.setDisable(true);
            targetAltitudeField.setDisable(true);
        }
        else
        {
            initialAltitudeField.setDisable(false);
            targetAltitudeField.setDisable(false);
        }
        String currentOrigin = originBox.getValue();
        String currentDestination = destinationBox.getValue();
        if (mode == DashboardMode.INTERPLANETARY_TRANSFER && currentOrigin != null && currentDestination != null
                && !currentOrigin.equalsIgnoreCase(currentDestination))
            modeDescription.setText("Recommended values refreshed for " + currentOrigin + " → " + currentDestination + ". Departure/arrival altitude fields are disabled because the heliocentric mode uses planetary ephemeris states.");
    }

    private String formatInput(double value)
    {
        return String.format(Locale.US, "%.3f", value).replaceAll("\\.?0+$", "");
    }

    private double roundUp(double value, double increment)
    {
        return Math.ceil(value / increment) * increment;
    }

    private double calculatePlannedRuntime(DashboardMode mode, double initialAltitude, double targetAltitude)
    {
        if (mode == DashboardMode.EARTH_ORBIT)
        {
            double radius = Constants.EARTH_RADIUS + initialAltitude;
            double period = Constants.TWO_PI * Math.sqrt(radius * radius * radius / Constants.EARTH_MU);
            return 10.0 * period; // ten-orbit visualisation window
        }
        if (mode == DashboardMode.HOHMANN_TRANSFER)
        {
            double transfer = new HohmannTransfer().calculateTransferTime(new Orbit(initialAltitude), new Orbit(targetAltitude));
            double targetRadius = Constants.EARTH_RADIUS + targetAltitude;
            double targetPeriod = Constants.TWO_PI * Math.sqrt(targetRadius * targetRadius * targetRadius / Constants.EARTH_MU);
            return transfer + 3.0 * targetPeriod;
        }
        if (mode == DashboardMode.LUNAR_TRANSFER)
        {
            if (currentLunarPlan == null)
                currentLunarPlan = new LunarMissionCalculator().calculateTransfer(initialAltitude, targetAltitude / 1000.0);
            double transfer = activeEngine != null && Double.isFinite(activeEngine.getPlannedTransferSeconds())
                ? activeEngine.getPlannedTransferSeconds() : currentLunarPlan.getTransferTimeSeconds();
            double orbitPeriod = activeEngine != null ? activeEngine.getCaptureOrbitPeriodSeconds() : Double.NaN;
            return transfer + (Double.isFinite(orbitPeriod) ? 3.0 * orbitPeriod : 0.0);
        }
        if (currentInterplanetaryPlan == null)
            currentInterplanetaryPlan = new InterplanetaryTransfer().calculateTransfer(
                originBox.getValue(), destinationBox.getValue(), departureDatePicker.getValue().atTime(12, 0));
        double transfer = activeEngine != null && Double.isFinite(activeEngine.getPlannedTransferSeconds())
            ? activeEngine.getPlannedTransferSeconds() : currentInterplanetaryPlan.getTransferTimeSeconds();
        double orbitPeriod = activeEngine != null ? activeEngine.getCaptureOrbitPeriodSeconds() : Double.NaN;
        return transfer + (Double.isFinite(orbitPeriod) ? 3.0 * orbitPeriod : 0.0);
    }

    private String describeRuntime(DashboardMode mode)
    {
        if (mode == DashboardMode.EARTH_ORBIT) return "10-orbit demonstration";
        if (mode == DashboardMode.HOHMANN_TRANSFER) return "Transfer plus three target-orbit revolutions";
        if (mode == DashboardMode.LUNAR_TRANSFER) return "Lunar transfer plus three captured orbits";
        return "Interplanetary transfer plus three captured orbits";
    }

    private void finishPlannedRuntime()
    {
        if (runtimeWindowCompleted || activeEngine == null) return;
        runtimeWindowCompleted = true;
        simulationPlaying = false;
        activeEngine.pause();
        runButton.setDisable(true);
        if (activeMode == DashboardMode.EARTH_ORBIT)
        {
            setStatus("ORBIT DEMONSTRATION WINDOW COMPLETE", "#69D8F2");
            phaseValue.setText("TEN-ORBIT DEMONSTRATION COMPLETE");
        }
        else if (activeMode == DashboardMode.HOHMANN_TRANSFER && activeEngine.isTransferComplete())
        {
            setStatus("HOHMANN TRANSFER COMPLETE", "#83F0BB");
            phaseValue.setText("TARGET ORBIT ESTABLISHED — THREE-ORBIT DEMONSTRATION COMPLETE");
        }
        else if (activeMode == DashboardMode.HOHMANN_TRANSFER)
        {
            setStatus("HOHMANN RUN WINDOW COMPLETE", "#FFDA7B");
            phaseValue.setText("PLANNED WINDOW ENDED — TARGET ORBIT ARRIVAL NOT CONFIRMED");
        }
        else if (activeMode == DashboardMode.LUNAR_TRANSFER)
        {
            if (activeEngine.isOrbitCaptured())
            {
                setStatus("MISSION SUCCESS — LUNAR ORBIT ESTABLISHED", "#83F0BB");
                phaseValue.setText("MOON CAPTURED — THREE-ORBIT DEMONSTRATION COMPLETE");
            }
            else
            {
                setStatus("MISSION INCOMPLETE — LUNAR CAPTURE NOT ACHIEVED", "#FFDA7B");
                phaseValue.setText("TRANSFER WINDOW ENDED WITHOUT A CONFIRMED LUNAR ORBIT");
            }
        }
        else
        {
            if (activeEngine.isOrbitCaptured())
            {
                setStatus("MISSION SUCCESS — " + activeEngine.getCapturedBodyName().toUpperCase(Locale.US) + " ORBIT ESTABLISHED", "#83F0BB");
                phaseValue.setText("DESTINATION CAPTURED — THREE-ORBIT DEMONSTRATION COMPLETE");
            }
            else
            {
                setStatus("MISSION INCOMPLETE — DESTINATION CAPTURE NOT ACHIEVED", "#FFDA7B");
                phaseValue.setText("TRANSFER WINDOW ENDED WITHOUT A CONFIRMED DESTINATION ORBIT");
            }
        }
        refreshDisplay();
    }

    private void runSimulation()
    {
        if (activeEngine == null) return;
        if (!planFeasible)
        {
            showPage(true);
            showError("The current vehicle does not have enough mathematically available delta-v/propellant for this mission model. Review Mission Analysis and increase the vehicle capability or apply the recommended values before running.");
            return;
        }
        if (runtimeWindowCompleted)
        {
            showError("The programmed simulation window is complete. Press RESET to run this mission again.");
            return;
        }
        if (activeEngine.isCollisionDetected())
        {
            showError("Reset the mission before continuing after an impact condition.");
            return;
        }
        activeEngine.resume();
        simulationPlaying = true;
        lastFrameNanos = 0L;
        setStatus("TIME-STEPPED SIMULATION RUNNING", "#83F0BB");
    }

    private void pauseSimulation()
    {
        simulationPlaying = false;
        if (activeEngine != null) activeEngine.pause();
        setStatus("SIMULATION PAUSED", "#FFDA7B");
        refreshDisplay();
    }

    private void resetSimulation()
    {
        if (activeEngine == null) return;
        initializeMission();
        if (activeEngine != null)
        {
            simulationPlaying = false;
            activeEngine.pause();
            setStatus("SIMULATION RESET — PRESS RUN", "#69D8F2");
        }
    }

    private void modeChanged(DashboardMode mode)
    {
        if (activeEngine != null) activeEngine.stop();
        activeEngine = null;
        activeMode = null;
        simulationPlaying = false;
        lastEventCount = -1;
        setButtonsInitialized(false);
        updateModeControls(mode);
        setupSpeedChoices(mode);
        setTimeStepChoices(mode);
        applyRecommendedValues(mode, true);
        currentLunarPlan = null;
        currentInterplanetaryPlan = null;
        plannedRuntimeSeconds = Double.NaN;
        plannedRuntimeDescription = "--";
        plannedRuntimeValue.setText("--");
        updateNavigationStyle(analysisPage != null && analysisPage.isVisible());
        clearTelemetry();
        eventList.getItems().clear();
        topStatus.setText("MODE SELECTED — INITIALIZE TO START");
        topStatus.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #69D8F2;");
        drawTrajectory();
    }

    private void updateModeControls(DashboardMode mode)
    {
        boolean interplanetary = mode == DashboardMode.INTERPLANETARY_TRANSFER;
        initialAltitudeField.setDisable(interplanetary);
        targetAltitudeField.setDisable(interplanetary);
        if (initialAltitudeCaption != null && targetAltitudeCaption != null)
        {
            if (mode == DashboardMode.EARTH_ORBIT)
            {
                initialAltitudeCaption.setText("Initial circular altitude (m)");
                targetAltitudeCaption.setText("Reference target altitude (m)");
            }
            else if (mode == DashboardMode.HOHMANN_TRANSFER)
            {
                initialAltitudeCaption.setText("Initial orbit altitude (m)");
                targetAltitudeCaption.setText("Target orbit altitude (m)");
            }
            else if (mode == DashboardMode.LUNAR_TRANSFER)
            {
                initialAltitudeCaption.setText("Earth parking-orbit altitude (m)");
                targetAltitudeCaption.setText("Target lunar-orbit altitude (m)");
            }
            else
            {
                initialAltitudeCaption.setText("Departure altitude (not used here)");
                targetAltitudeCaption.setText("Arrival altitude (not used here)");
            }
        }
        if (mode == DashboardMode.EARTH_ORBIT)
            modeDescription.setText("Earth-centred circular orbit; the spacecraft is propagated with RK4 gravity integration for a finite orbital demonstration window.");
        else if (mode == DashboardMode.HOHMANN_TRANSFER)
            modeDescription.setText("Earth-centred transfer arc; the second impulsive circularization is applied at the target orbit and the run stops at transfer completion.");
        else if (mode == DashboardMode.LUNAR_TRANSFER)
            modeDescription.setText("Earth–Moon transfer with a moving prescribed Moon orbit. The Moon is not pulled toward Earth by the spacecraft; scale remains fixed and lunar impact halts the run.");
        else
            modeDescription.setText("Heliocentric transfer; planet choices automatically update recommended values and launch-window planning. The plot scale remains stable as bodies move.");
    }

    private void setTimeStepChoices(DashboardMode mode)
    {
        List<Double> values;
        double defaultStep;
        if (mode == DashboardMode.EARTH_ORBIT)
        {
            values = Arrays.asList(0.5, 1.0, 2.0, 5.0, 10.0, 30.0);
            defaultStep = 5.0;
        }
        else if (mode == DashboardMode.HOHMANN_TRANSFER)
        {
            values = Arrays.asList(0.25, 0.5, 1.0, 2.0, 5.0, 10.0);
            defaultStep = 2.0;
        }
        else if (mode == DashboardMode.LUNAR_TRANSFER)
        {
            values = Arrays.asList(0.5, 1.0, 2.0, 5.0, 10.0, 30.0);
            defaultStep = 5.0;
        }
        else
        {
            values = Arrays.asList(60.0, 300.0, 600.0, 1200.0, 1800.0, 3600.0);
            defaultStep = 3600.0;
        }
        timeStepBox.setItems(FXCollections.observableArrayList(values));
        timeStepBox.setValue(defaultStep);
    }

    private void setupSpeedChoices(DashboardMode mode)
    {
        List<Double> scales = Arrays.asList(1.0, 2.0, 3.0, 5.0, 10.0, 100.0, 1000.0, 10000.0, 100000.0, 1000000.0);
        speedBox.setItems(FXCollections.observableArrayList(scales));
        // Start fast as requested; users can select 2x, 3x, or another scale before running.
        speedBox.setValue(100000.0);
        lastSimulationSpeed = 100000.0;
    }

    private double selectedSpeed()
    {
        Double speed = speedBox.getValue();
        return speed == null ? 1.0 : speed.doubleValue();
    }

    /** Choose a fast scale that leaves about twenty seconds to show three revolutions. */
    private double recommendedOrbitDemoSpeed(double orbitPeriodSeconds)
    {
        double desired = 3.0 * orbitPeriodSeconds / 20.0;
        double[] choices = {1000.0, 10000.0, 100000.0};
        double best = choices[0];
        double bestError = Math.abs(Math.log(Math.max(1.0, desired) / best));
        for (int i = 1; i < choices.length; i++)
        {
            double error = Math.abs(Math.log(Math.max(1.0, desired) / choices[i]));
            if (error < bestError)
            {
                best = choices[i];
                bestError = error;
            }
        }
        return best;
    }

    private void refreshDisplay()
    {
        if (activeEngine == null)
        {
            drawTrajectory();
            return;
        }
        ForgeOrbitTimeStepEngine.Snapshot state = activeEngine.snapshot();
        elapsedValue.setText(formatDuration(state.getElapsedSeconds()));
        stepCountValue.setText(String.format(Locale.US, "%,d", state.getStepCount()));
        Vector3 p = state.getPositionMetres();
        Vector3 v = state.getVelocityMetresPerSecond();
        Vector3 a = state.getAccelerationMetresPerSecondSquared();
        positionValue.setText(formatVector(p));
        velocityValue.setText(formatVector(v));
        speedValue.setText(format(state.getSpeedMetresPerSecond(), "m/s"));
        accelerationValue.setText(format(a.magnitude(), "m/s²"));
        radiusValue.setText(formatDistance(state.getRadiusMetres()));
        altitudeValue.setText(Double.isFinite(state.getAltitudeMetres()) ? formatDistance(state.getAltitudeMetres()) : "N/A (heliocentric)");
        targetDistanceValue.setText(Double.isFinite(state.getTargetDistanceMetres()) ? formatDistance(state.getTargetDistanceMetres()) : "N/A for this mode");
        relativeSpeedValue.setText(Double.isFinite(state.getRelativeSpeedToTargetMetresPerSecond())
            ? format(state.getRelativeSpeedToTargetMetresPerSecond(), "m/s") : "N/A");
        energyValue.setText(formatScientific(state.getSpecificEnergyJPerKg()) + " J/kg");
        periodValue.setText(Double.isFinite(state.getOsculatingOrbitalPeriodSeconds())
            ? formatDuration(state.getOsculatingOrbitalPeriodSeconds()) : "N/A (unbound)");
        lastDeltaVValue.setText(format(state.getLastDeltaVMetresPerSecond(), "m/s"));

        if (state.isCollisionDetected())
            phaseValue.setText("SURFACE IMPACT — MISSION FAILED");
        else if (state.isOrbitCaptured())
            phaseValue.setText(activeEngine.getCapturedBodyName().toUpperCase(Locale.US)
                + " ORBIT CAPTURED — TARGET-CENTRED LIVE PROPAGATION");
        else if (state.getMode() == ForgeOrbitTimeStepEngine.Mode.HOHMANN_TRANSFER && state.isTransferComplete())
            phaseValue.setText("TRANSFER COMPLETE — CIRCULARIZATION APPLIED");
        else if (state.getMode() == ForgeOrbitTimeStepEngine.Mode.EARTH_ORBIT)
            phaseValue.setText("CIRCULAR EARTH ORBIT — LIVE PROPAGATION");
        else if (state.getMode() == ForgeOrbitTimeStepEngine.Mode.HOHMANN_TRANSFER)
            phaseValue.setText("HOHMANN TRANSFER COAST");
        else if (state.getMode() == ForgeOrbitTimeStepEngine.Mode.LUNAR_TRANSFER)
            phaseValue.setText("LUNAR TRANSFER — EARTH + MOVING MOON GRAVITY");
        else
            phaseValue.setText("HELIOCENTRIC TRANSFER COAST");

        if (state.isCollisionDetected()) setStatus("SURFACE IMPACT — MISSION FAILED", "#FF8A8A");
        else if (state.isOrbitCaptured() && simulationPlaying)
            setStatus("MISSION SUCCESS — ORBIT CAPTURED / ORBIT DEMO RUNNING", "#83F0BB");
        else if (state.isOrbitCaptured() && !runtimeWindowCompleted)
            setStatus("MISSION SUCCESS — ORBIT CAPTURED / PAUSED", "#83F0BB");
        else if (activeMode == DashboardMode.HOHMANN_TRANSFER && state.isTransferComplete() && simulationPlaying)
            setStatus("TARGET ORBIT ESTABLISHED — ORBIT DEMO RUNNING", "#83F0BB");
        else if (activeMode == DashboardMode.HOHMANN_TRANSFER && state.isTransferComplete() && !runtimeWindowCompleted)
            setStatus("TARGET ORBIT ESTABLISHED — PAUSED", "#83F0BB");
        else if (simulationPlaying) setStatus("TIME-STEPPED SIMULATION RUNNING", "#83F0BB");
        else if (!runtimeWindowCompleted) setStatus("SIMULATION PAUSED", "#FFDA7B");

        synchronizeEvents();
        drawTrajectory();
    }

    private void synchronizeEvents()
    {
        if (activeEngine == null) return;
        List<String> events = activeEngine.getEvents();
        if (events.size() != lastEventCount)
        {
            eventList.getItems().setAll(events);
            if (!events.isEmpty()) eventList.scrollTo(events.size() - 1);
            lastEventCount = events.size();
        }
    }

    private void drawTrajectory()
    {
        GraphicsContext g = trajectoryCanvas.getGraphicsContext2D();
        double w = trajectoryCanvas.getWidth();
        double h = trajectoryCanvas.getHeight();
        if (w < 10 || h < 10) return;
        g.setFill(Color.web("#07111F"));
        g.fillRect(0, 0, w, h);
        g.setStroke(Color.web("#13283E"));
        g.setLineWidth(1.0);
        for (double x = 20; x < w; x += 40) g.strokeLine(x, 0, x, h);
        for (double y = 20; y < h; y += 40) g.strokeLine(0, y, w, y);

        if (activeEngine == null)
        {
            g.setFill(Color.web("#7791AF"));
            g.setFont(Font.font("Consolas", FontWeight.NORMAL, 13));
            g.fillText("INITIALIZE A MISSION TO VIEW THE TRAJECTORY", 24, h / 2.0);
            g.setFont(Font.font("Consolas", FontWeight.NORMAL, 10));
            g.fillText("CYAN DASHED = INTENDED PATH   GREEN = ACTUAL PATH", 24, h / 2.0 + 24);
            return;
        }

        List<Vector3> planned = activeEngine.getPlannedTrajectory();
        List<Vector3> actual = activeEngine.getActualTrajectory();
        Vector3 target = activeEngine.getTargetPositionMetres();
        boolean orbitCaptured = activeEngine.isOrbitCaptured();
        boolean heliocentric = activeEngine.getMode() == ForgeOrbitTimeStepEngine.Mode.INTERPLANETARY_TRANSFER && !orbitCaptured;

        double maxRadius = 1.0;
        // Fit the world frame to the planned transfer, not the growing actual path.
        // Including actual points made the viewport zoom out every time a craft
        // travelled beyond its planned ellipse, which made the Moon/planet appear
        // to move toward the primary body even though the body model was not doing so.
        for (Vector3 p : planned) maxRadius = Math.max(maxRadius, Math.hypot(p.getX(), p.getY()));
        if (!heliocentric && !orbitCaptured) maxRadius = Math.max(maxRadius, Constants.EARTH_RADIUS);
        double extentFactor;
        if (orbitCaptured) extentFactor = 1.55;
        else if (heliocentric) extentFactor = 1.20;
        else if (activeEngine.getMode() == ForgeOrbitTimeStepEngine.Mode.LUNAR_TRANSFER) extentFactor = 1.12;
        else extentFactor = 2.0;
        double extent = Math.max(maxRadius * extentFactor, 1.0);
        double scale = Math.min(w, h) * 0.44 / extent;
        double cx = w * 0.5;
        double cy = h * 0.5;

        g.setStroke(Color.web("#203750"));
        g.setLineWidth(1.0);
        for (int i = 1; i <= 4; i++)
        {
            double r = Math.min(w, h) * 0.44 * i / 4.0;
            g.strokeOval(cx - r, cy - r, 2 * r, 2 * r);
        }
        g.setStroke(Color.web("#29435F"));
        g.strokeLine(0, cy, w, cy);
        g.strokeLine(cx, 0, cx, h);

        drawPath(g, planned, cx, cy, scale, Color.web("#36BFFA"), 1.8, true);
        drawPath(g, actual, cx, cy, scale, Color.web("#35D6A3"), 2.4, false);

        if (orbitCaptured)
        {
            String body = activeEngine.getCapturedBodyName() == null
                ? "TARGET" : activeEngine.getCapturedBodyName();
            double physicalRadius = activeEngine.getCapturedBodyRadiusMetres() * scale;
            double bodyRadius = Math.max(23.0, Math.min(68.0, physicalRadius));

            drawCelestialBody(g, body, cx, cy, bodyRadius);
            drawCelestialLabel(g, cx, cy, bodyRadius,
                body.toUpperCase(Locale.US) + " CAPTURE ORBIT", w, h,
                Color.web("#83F0BB"));

            g.setFill(Color.web("#83F0BB"));
            g.setFont(Font.font("Consolas", FontWeight.BOLD, 11));
            g.fillText("ORBIT INSERTION CONFIRMED", 12, 38);
        }
        else if (heliocentric)
        {
            double sunRadius = Math.max(18.0, Math.min(33.0, Math.min(w, h) * 0.055));
            drawCelestialBody(g, "Sun", cx, cy, sunRadius);
            drawCelestialLabel(g, cx, cy, sunRadius, "SUN", w, h,
                Color.web("#FFD166"));

            if (!actual.isEmpty())
            {
                Vector3 departure = actual.get(0);
                double px = cx + departure.getX() * scale;
                double py = cy - departure.getY() * scale;
                String originName = activeEngine.getOriginName();
                double planetRadius = getHeliocentricBodyRadius(originName);
                drawCelestialBody(g, originName, px, py, planetRadius);
                drawCelestialLabel(g, px, py, planetRadius,
                    originName.toUpperCase(Locale.US), w, h,
                    Color.web("#9DB7D5"));
            }

            if (target != null)
            {
                double px = cx + target.getX() * scale;
                double py = cy - target.getY() * scale;
                String destinationName = activeEngine.getDestinationName();
                double planetRadius = getHeliocentricBodyRadius(destinationName);
                drawCelestialBody(g, destinationName, px, py, planetRadius);
                drawCelestialLabel(g, px, py, planetRadius,
                    destinationName.toUpperCase(Locale.US), w, h,
                    Color.web("#FFB454"));
            }
        }
        else
        {
            // In wide Earth-Moon transfers the physical Earth radius can be only
            // a few pixels at the transfer scale. Use a minimum display radius so
            // the body remains readable; the trajectory coordinates stay unchanged.
            double earthRadius = Math.max(18.0,
                Math.min(84.0, Constants.EARTH_RADIUS * scale));
            drawCelestialBody(g, "Earth", cx, cy, earthRadius);
            drawCelestialLabel(g, cx, cy, earthRadius, "EARTH", w, h,
                Color.web("#8FD7FF"));

            if (activeEngine.getMode() == ForgeOrbitTimeStepEngine.Mode.LUNAR_TRANSFER && target != null)
            {
                double moonX = cx + target.getX() * scale;
                double moonY = cy - target.getY() * scale;
                double moonRadius = Math.max(7.0,
                    Math.min(18.0, 1737400.0 * scale));
                drawCelestialBody(g, "Moon", moonX, moonY, moonRadius);
                drawCelestialLabel(g, moonX, moonY, moonRadius, "MOON", w, h,
                    Color.web("#D4DCE7"));
            }
            else if (activeEngine.getMode() == ForgeOrbitTimeStepEngine.Mode.HOHMANN_TRANSFER && !planned.isEmpty())
            {
                Vector3 arrival = planned.get(planned.size() - 1);
                drawMarker(g, cx + arrival.getX() * scale,
                    cy - arrival.getY() * scale, 4,
                    Color.web("#FFB454"), "TARGET ORBIT");
            }
        }

        g.setFill(Color.web("#D7E8FA"));
        g.setFont(Font.font("Consolas", FontWeight.BOLD, 10));
        String frame = orbitCaptured ? activeEngine.getCapturedBodyName().toUpperCase(Locale.US) + "-CENTRED CAPTURE ORBIT"
            : (heliocentric ? "HELIOCENTRIC FRAME" : "EARTH-CENTRED FRAME");
        g.fillText(frame, 12, 18);
        g.setFont(Font.font("Consolas", FontWeight.NORMAL, 9));
        g.setFill(Color.web("#7894B5"));
        g.fillText("t = " + formatDuration(activeEngine.getElapsedSeconds())
            + "   RK4 STEPS = " + String.format(Locale.US, "%,d", activeEngine.getStepCount()), 12, h - 12);
    }

    /**
     * Draws a stylised, shaded celestial body without changing simulation data.
     * Sizes may be visually exaggerated in wide-range plots so bodies remain legible.
     */
    private void drawCelestialBody(GraphicsContext g, String bodyName,
                                   double cx, double cy, double radius)
    {
        if (bodyName == null || !Double.isFinite(cx) || !Double.isFinite(cy)
                || !Double.isFinite(radius) || radius <= 1.0) return;

        String body = bodyName.trim().toUpperCase(Locale.US);
        if ("SUN".equals(body))
        {
            drawSun(g, cx, cy, radius);
            return;
        }

        Color glow = Color.web("#7CA7D8");
        Color highlight = Color.web("#DDEBFA");
        Color base = Color.web("#7890A8");
        Color shadow = Color.web("#101827");

        if ("EARTH".equals(body))
        {
            glow = Color.web("#32B9F2");
            highlight = Color.web("#7BD7F7");
            base = Color.web("#155786");
            shadow = Color.web("#061327");
        }
        else if ("MOON".equals(body))
        {
            glow = Color.web("#9BAFC5");
            highlight = Color.web("#F0F2F5");
            base = Color.web("#929DA9");
            shadow = Color.web("#343D4A");
        }
        else if ("MERCURY".equals(body))
        {
            glow = Color.web("#A6A7A5");
            highlight = Color.web("#DBD4CB");
            base = Color.web("#817D79");
            shadow = Color.web("#302E32");
        }
        else if ("VENUS".equals(body))
        {
            glow = Color.web("#F3C276");
            highlight = Color.web("#FFF0BC");
            base = Color.web("#D9A45B");
            shadow = Color.web("#704529");
        }
        else if ("MARS".equals(body)
                || "PHOBOS".equals(body) || "DEIMOS".equals(body))
        {
            glow = Color.web("#D66C50");
            highlight = Color.web("#F9B18A");
            base = Color.web("#B9573B");
            shadow = Color.web("#421D25");
        }
        else if ("JUPITER".equals(body))
        {
            glow = Color.web("#D7AE85");
            highlight = Color.web("#FFE2B8");
            base = Color.web("#C18D63");
            shadow = Color.web("#54392D");
        }
        else if ("SATURN".equals(body))
        {
            glow = Color.web("#D9BD84");
            highlight = Color.web("#FFF0C5");
            base = Color.web("#CDB17A");
            shadow = Color.web("#55442D");
        }
        else if ("URANUS".equals(body))
        {
            glow = Color.web("#6EDDD9");
            highlight = Color.web("#C7FFF5");
            base = Color.web("#65C6C6");
            shadow = Color.web("#1A4A62");
        }
        else if ("NEPTUNE".equals(body))
        {
            glow = Color.web("#4D83FF");
            highlight = Color.web("#A6C8FF");
            base = Color.web("#2754B7");
            shadow = Color.web("#101B53");
        }

        drawBodyGlow(g, cx, cy, radius, glow);
        if ("SATURN".equals(body)) drawSaturnRings(g, cx, cy, radius, false);

        RadialGradient sphere = new RadialGradient(
            0.0, 0.0,
            cx - radius * 0.34,
            cy - radius * 0.42,
            radius * 1.58,
            false,
            CycleMethod.NO_CYCLE,
            new Stop(0.0, highlight),
            new Stop(0.53, base),
            new Stop(1.0, shadow)
        );
        g.setFill(sphere);
        g.fillOval(cx - radius, cy - radius, radius * 2.0, radius * 2.0);

        g.save();
        g.beginPath();
        g.arc(cx, cy, radius, radius, 0, 360);
        g.closePath();
        g.clip();

        if ("EARTH".equals(body)) drawEarthSurface(g, cx, cy, radius);
        else if ("MOON".equals(body) || "MERCURY".equals(body))
            drawCrateredSurface(g, cx, cy, radius, "MOON".equals(body));
        else if ("MARS".equals(body)) drawMarsSurface(g, cx, cy, radius);
        else if ("VENUS".equals(body)) drawVenusSurface(g, cx, cy, radius);
        else if ("JUPITER".equals(body)) drawGasGiantSurface(g, cx, cy, radius, true);
        else if ("SATURN".equals(body)) drawGasGiantSurface(g, cx, cy, radius, false);
        else if ("URANUS".equals(body) || "NEPTUNE".equals(body))
            drawIceGiantSurface(g, cx, cy, radius, "NEPTUNE".equals(body));
        else drawCrateredSurface(g, cx, cy, radius, false);

        // A soft terminator-side shadow gives small bodies a spherical silhouette.
        g.setFill(new RadialGradient(0.0, 0.0,
            cx + radius * 0.52, cy + radius * 0.16, radius * 1.18,
            false, CycleMethod.NO_CYCLE,
            new Stop(0.0, Color.color(0.015, 0.035, 0.075, 0.02)),
            new Stop(0.72, Color.color(0.015, 0.035, 0.075, 0.16)),
            new Stop(1.0, Color.color(0.01, 0.02, 0.05, 0.70))));
        g.fillOval(cx - radius, cy - radius, radius * 2.0, radius * 2.0);
        g.restore();

        g.setStroke(Color.color(0.74, 0.86, 1.0, 0.52));
        g.setLineWidth(Math.max(0.7, radius * 0.018));
        g.strokeOval(cx - radius, cy - radius, radius * 2.0, radius * 2.0);
        g.setStroke(Color.color(highlight.getRed(), highlight.getGreen(), highlight.getBlue(), 0.30));
        g.setLineWidth(Math.max(0.6, radius * 0.012));
        g.strokeArc(cx - radius * 0.91, cy - radius * 0.91,
            radius * 1.82, radius * 1.82, 25, 105, ArcType.OPEN);

        if ("SATURN".equals(body)) drawSaturnRings(g, cx, cy, radius, true);
    }

    private void drawBodyGlow(GraphicsContext g, double cx, double cy,
                              double radius, Color glow)
    {
        for (int i = 5; i >= 1; i--)
        {
            double spread = 1.0 + i * 0.09;
            double alpha = 0.011 * (6 - i);
            g.setFill(Color.color(glow.getRed(), glow.getGreen(), glow.getBlue(), alpha));
            g.fillOval(cx - radius * spread, cy - radius * spread,
                radius * 2.0 * spread, radius * 2.0 * spread);
        }
    }

    private void drawSun(GraphicsContext g, double cx, double cy, double radius)
    {
        Color glow = Color.web("#FF9D32");
        drawBodyGlow(g, cx, cy, radius * 1.10, glow);
        for (int i = 0; i < 20; i++)
        {
            double angle = Math.PI * 2.0 * i / 20.0;
            double inner = radius * 1.07;
            double outer = radius * (1.20 + (i % 2 == 0 ? 0.18 : 0.06));
            g.setStroke(Color.color(1.0, 0.57, 0.16, i % 2 == 0 ? 0.46 : 0.24));
            g.setLineWidth(i % 2 == 0 ? 1.3 : 0.7);
            g.strokeLine(cx + Math.cos(angle) * inner, cy + Math.sin(angle) * inner,
                cx + Math.cos(angle) * outer, cy + Math.sin(angle) * outer);
        }
        g.setFill(new RadialGradient(0.0, 0.0, cx - radius * 0.27,
            cy - radius * 0.33, radius * 1.35, false, CycleMethod.NO_CYCLE,
            new Stop(0.0, Color.web("#FFF9C4")),
            new Stop(0.40, Color.web("#FFD45A")),
            new Stop(0.78, Color.web("#FF8D28")),
            new Stop(1.0, Color.web("#A93612"))));
        g.fillOval(cx - radius, cy - radius, radius * 2.0, radius * 2.0);
        g.save();
        g.beginPath(); g.arc(cx, cy, radius, radius, 0, 360); g.closePath(); g.clip();
        g.setStroke(Color.color(1.0, 0.88, 0.43, 0.48));
        g.setLineWidth(Math.max(0.7, radius * 0.045));
        g.strokeArc(cx - radius * 0.72, cy - radius * 0.68,
            radius * 1.44, radius * 1.36, 25, 125, ArcType.OPEN);
        g.strokeArc(cx - radius * 0.53, cy - radius * 0.42,
            radius * 1.05, radius * 0.94, 205, 95, ArcType.OPEN);
        g.restore();
        g.setStroke(Color.web("#FFD166"));
        g.setLineWidth(1.0);
        g.strokeOval(cx - radius, cy - radius, radius * 2.0, radius * 2.0);
    }

    private void drawEarthSurface(GraphicsContext g, double cx, double cy, double radius)
    {
        g.setFill(Color.rgb(34, 148, 179, 0.44));
        g.fillOval(cx - radius * 0.77, cy - radius * 0.74,
            radius * 1.66, radius * 1.43);
        g.setFill(Color.rgb(59, 166, 125, 0.96));
        drawSurfacePolygon(g, cx, cy, radius,
            new double[]{-0.83,-0.68,-0.47,-0.31,-0.36,-0.49,-0.62,-0.74},
            new double[]{-0.45,-0.68,-0.72,-0.51,-0.29,-0.16,-0.28,-0.30});
        drawSurfacePolygon(g, cx, cy, radius,
            new double[]{-0.38,-0.20,-0.08,-0.15,-0.26,-0.37,-0.42},
            new double[]{0.02,0.05,0.24,0.47,0.73,0.47,0.20});
        drawSurfacePolygon(g, cx, cy, radius,
            new double[]{0.05,0.22,0.46,0.69,0.72,0.53,0.37,0.19},
            new double[]{-0.53,-0.66,-0.53,-0.30,-0.06,0.04,-0.12,-0.20});
        drawSurfacePolygon(g, cx, cy, radius,
            new double[]{0.12,0.37,0.43,0.32,0.22,0.08},
            new double[]{-0.04,0.01,0.25,0.53,0.66,0.32});

        g.setStroke(Color.rgb(178, 235, 248, 0.34));
        g.setLineWidth(Math.max(0.55, radius * 0.013));
        for (int i = -1; i <= 1; i++)
        {
            double fraction = i * 0.43;
            double halfWidth = radius * Math.sqrt(1.0 - fraction * fraction);
            g.strokeLine(cx - halfWidth, cy + fraction * radius,
                cx + halfWidth, cy + fraction * radius);
        }
        g.strokeOval(cx - radius * 0.43, cy - radius, radius * 0.86, radius * 2.0);

        g.setStroke(Color.rgb(239, 248, 255, 0.40));
        g.setLineWidth(Math.max(0.65, radius * 0.025));
        g.strokeArc(cx - radius * 0.78, cy - radius * 0.73,
            radius * 1.54, radius * 1.46, 210, 82, ArcType.OPEN);
        g.strokeArc(cx - radius * 0.60, cy - radius * 0.52,
            radius * 1.25, radius * 1.05, 30, 93, ArcType.OPEN);
        g.setFill(Color.rgb(229, 246, 252, 0.16));
        g.fillOval(cx - radius * 0.40, cy - radius * 0.10,
            radius * 0.54, radius * 0.11);
        g.fillOval(cx + radius * 0.12, cy + radius * 0.35,
            radius * 0.38, radius * 0.09);
    }

    private void drawSurfacePolygon(GraphicsContext g, double cx, double cy, double radius,
                                    double[] xFractions, double[] yFractions)
    {
        int count = Math.min(xFractions.length, yFractions.length);
        if (count < 3) return;
        double[] xPoints = new double[count];
        double[] yPoints = new double[count];
        for (int i = 0; i < count; i++)
        {
            xPoints[i] = cx + xFractions[i] * radius;
            yPoints[i] = cy + yFractions[i] * radius;
        }
        g.fillPolygon(xPoints, yPoints, count);
    }

    private void drawCrateredSurface(GraphicsContext g, double cx, double cy,
                                     double radius, boolean lunar)
    {
        Color shadow = lunar ? Color.rgb(44, 51, 60, 0.47) : Color.rgb(49, 43, 43, 0.43);
        Color rim = lunar ? Color.rgb(238, 240, 242, 0.34) : Color.rgb(230, 216, 205, 0.25);
        double[][] craters = {
            {-0.48,-0.40,0.16}, {-0.12,-0.54,0.10}, {0.31,-0.43,0.13},
            {0.53,-0.10,0.17}, {0.08,-0.07,0.20}, {-0.43,0.15,0.12},
            {-0.16,0.43,0.15}, {0.28,0.47,0.10}, {0.45,0.30,0.065},
            {-0.03,0.66,0.06}, {-0.64,-0.05,0.055}
        };
        for (double[] crater : craters)
        {
            double x = cx + crater[0] * radius;
            double y = cy + crater[1] * radius;
            double r = crater[2] * radius;
            g.setFill(shadow);
            g.fillOval(x - r, y - r * 0.83, r * 2.0, r * 1.66);
            g.setStroke(rim);
            g.setLineWidth(Math.max(0.45, radius * 0.012));
            g.strokeOval(x - r * 0.92, y - r * 0.93, r * 1.84, r * 1.70);
            g.setFill(Color.rgb(255, 255, 255, lunar ? 0.10 : 0.07));
            g.fillOval(x - r * 0.50, y - r * 0.53, r * 0.76, r * 0.38);
        }
        if (lunar)
        {
            g.setFill(Color.rgb(247, 248, 250, 0.11));
            g.fillOval(cx - radius * 0.52, cy - radius * 0.71,
                radius * 0.75, radius * 0.40);
        }
    }

    private void drawMarsSurface(GraphicsContext g, double cx, double cy, double radius)
    {
        g.setFill(Color.rgb(100, 43, 35, 0.37));
        g.fillOval(cx - radius * 0.78, cy - radius * 0.34,
            radius * 1.12, radius * 0.40);
        g.fillOval(cx + radius * 0.02, cy + radius * 0.17,
            radius * 0.68, radius * 0.35);
        g.setFill(Color.rgb(239, 151, 103, 0.34));
        g.fillOval(cx - radius * 0.56, cy + radius * 0.10,
            radius * 0.64, radius * 0.16);
        g.setFill(Color.rgb(255, 229, 195, 0.70));
        g.fillOval(cx - radius * 0.34, cy - radius * 0.91,
            radius * 0.69, radius * 0.15);
        g.setStroke(Color.rgb(77, 35, 31, 0.35));
        g.setLineWidth(Math.max(0.55, radius * 0.018));
        g.strokeArc(cx - radius * 0.70, cy - radius * 0.54,
            radius * 1.15, radius * 0.96, 35, 120, ArcType.OPEN);
        g.strokeArc(cx - radius * 0.26, cy + radius * 0.02,
            radius * 0.94, radius * 0.65, 200, 115, ArcType.OPEN);
    }

    private void drawVenusSurface(GraphicsContext g, double cx, double cy, double radius)
    {
        g.setStroke(Color.rgb(255, 234, 174, 0.40));
        g.setLineWidth(Math.max(0.8, radius * 0.065));
        for (int i = -2; i <= 2; i++)
        {
            double y = cy + i * radius * 0.29;
            g.strokeArc(cx - radius * 1.04, y - radius * 0.30,
                radius * 2.08, radius * 0.60, 7 + i * 11, 160, ArcType.OPEN);
        }
        g.setStroke(Color.rgb(137, 88, 41, 0.25));
        g.setLineWidth(Math.max(0.5, radius * 0.018));
        g.strokeArc(cx - radius * 0.69, cy - radius * 0.51,
            radius * 1.34, radius * 1.05, 185, 155, ArcType.OPEN);
    }

    private void drawGasGiantSurface(GraphicsContext g, double cx, double cy,
                                     double radius, boolean jupiter)
    {
        Color[] bands = jupiter
            ? new Color[]{Color.rgb(111, 73, 54, 0.32), Color.rgb(255, 221, 177, 0.43),
                Color.rgb(163, 108, 75, 0.43), Color.rgb(250, 218, 180, 0.28)}
            : new Color[]{Color.rgb(111, 83, 43, 0.23), Color.rgb(255, 234, 183, 0.35),
                Color.rgb(163, 119, 62, 0.30), Color.rgb(246, 219, 161, 0.26)};
        double[] offsets = {-0.72, -0.49, -0.23, 0.02, 0.30, 0.55, 0.74};
        g.setLineWidth(Math.max(1.0, radius * (jupiter ? 0.10 : 0.12)));
        for (int i = 0; i < offsets.length; i++)
        {
            g.setStroke(bands[i % bands.length]);
            double y = cy + offsets[i] * radius;
            g.strokeLine(cx - radius, y, cx + radius, y + radius * 0.025);
        }
        if (jupiter)
        {
            g.setFill(Color.rgb(170, 75, 49, 0.83));
            g.fillOval(cx + radius * 0.10, cy + radius * 0.16,
                radius * 0.48, radius * 0.23);
            g.setStroke(Color.rgb(242, 178, 132, 0.52));
            g.setLineWidth(Math.max(0.5, radius * 0.018));
            g.strokeOval(cx + radius * 0.13, cy + radius * 0.17,
                radius * 0.42, radius * 0.20);
        }
    }

    private void drawIceGiantSurface(GraphicsContext g, double cx, double cy,
                                     double radius, boolean neptune)
    {
        Color lightBand = neptune ? Color.rgb(145, 184, 255, 0.30)
            : Color.rgb(192, 255, 245, 0.34);
        Color darkBand = neptune ? Color.rgb(9, 38, 112, 0.28)
            : Color.rgb(28, 105, 122, 0.24);
        g.setLineWidth(Math.max(0.9, radius * 0.09));
        for (int i = -2; i <= 2; i++)
        {
            g.setStroke(i % 2 == 0 ? lightBand : darkBand);
            double y = cy + i * radius * 0.29;
            g.strokeArc(cx - radius * 1.03, y - radius * 0.20,
                radius * 2.06, radius * 0.42, 6 + i * 7, 168, ArcType.OPEN);
        }
        if (neptune)
        {
            g.setFill(Color.rgb(15, 31, 89, 0.46));
            g.fillOval(cx + radius * 0.13, cy + radius * 0.10,
                radius * 0.29, radius * 0.17);
        }
    }

    private void drawSaturnRings(GraphicsContext g, double cx, double cy,
                                 double radius, boolean foreground)
    {
        g.save();
        g.setLineWidth(Math.max(1.0, radius * 0.115));
        g.setStroke(Color.color(0.78, 0.65, 0.43, foreground ? 0.72 : 0.46));
        if (foreground)
            g.strokeArc(cx - radius * 2.02, cy - radius * 0.60,
                radius * 4.04, radius * 1.20, 195, 150, ArcType.OPEN);
        else
            g.strokeOval(cx - radius * 2.02, cy - radius * 0.60,
                radius * 4.04, radius * 1.20);
        g.setLineWidth(Math.max(0.55, radius * 0.028));
        g.setStroke(Color.color(0.96, 0.84, 0.62, foreground ? 0.88 : 0.61));
        if (foreground)
            g.strokeArc(cx - radius * 1.72, cy - radius * 0.49,
                radius * 3.44, radius * 0.98, 195, 150, ArcType.OPEN);
        else
            g.strokeOval(cx - radius * 1.72, cy - radius * 0.49,
                radius * 3.44, radius * 0.98);
        g.restore();
    }

    private double getHeliocentricBodyRadius(String bodyName)
    {
        if (bodyName == null) return 6.0;
        String body = bodyName.toUpperCase(Locale.US);
        if ("JUPITER".equals(body)) return 11.0;
        if ("SATURN".equals(body)) return 10.0;
        if ("EARTH".equals(body) || "VENUS".equals(body)) return 8.0;
        if ("URANUS".equals(body) || "NEPTUNE".equals(body)) return 8.5;
        if ("MERCURY".equals(body) || "MARS".equals(body)) return 6.5;
        return 5.0;
    }

    private void drawCelestialLabel(GraphicsContext g, double x, double y, double radius,
                                    String label, double width, double height, Color accent)
    {
        if (label == null || label.trim().isEmpty()
                || !Double.isFinite(x) || !Double.isFinite(y)) return;
        String text = label.toUpperCase(Locale.US);
        double textWidth = text.length() * 5.7 + 14.0;
        double textX = x + radius + 7.0;
        if (textX + textWidth > width - 5.0) textX = x - radius - textWidth - 7.0;
        textX = Math.max(5.0, Math.min(width - textWidth - 5.0, textX));
        double baseline = y - radius - 6.0;
        if (baseline < 15.0) baseline = Math.min(height - 20.0, y + radius + 17.0);
        g.setFill(Color.rgb(4, 12, 22, 0.88));
        g.fillRoundRect(textX - 5.0, baseline - 12.0, textWidth, 17.0, 6.0, 6.0);
        g.setFill(accent == null ? Color.web("#D7E8FA") : accent);
        g.setFont(Font.font("Consolas", FontWeight.BOLD, 8.5));
        g.fillText(text, textX + 2.0, baseline);
    }

    private void drawPath(GraphicsContext g, List<Vector3> points, double cx, double cy,
                          double scale, Color color, double lineWidth, boolean dashed)
    {
        if (points == null || points.size() < 2) return;
        g.setStroke(color);
        g.setLineWidth(lineWidth);
        g.setLineDashes(dashed ? new double[]{7.0, 5.0} : new double[]{});
        Vector3 previous = points.get(0);
        for (int i = 1; i < points.size(); i++)
        {
            Vector3 next = points.get(i);
            double x1 = cx + previous.getX() * scale;
            double y1 = cy - previous.getY() * scale;
            double x2 = cx + next.getX() * scale;
            double y2 = cy - next.getY() * scale;
            if (Double.isFinite(x1) && Double.isFinite(y1) && Double.isFinite(x2) && Double.isFinite(y2))
                g.strokeLine(x1, y1, x2, y2);
            previous = next;
        }
        g.setLineDashes();
    }

    private void drawMarker(GraphicsContext g, double x, double y, double radius, Color color, String label)
    {
        if (!Double.isFinite(x) || !Double.isFinite(y)) return;
        g.setFill(color);
        g.fillOval(x - radius, y - radius, radius * 2, radius * 2);
        g.setFill(Color.web("#E5EDF7"));
        g.setFont(Font.font("Consolas", FontWeight.BOLD, 9));
        g.fillText(label, x + radius + 4, y - radius - 3);
    }

    private void clearTelemetry()
    {
        for (Label label : Arrays.asList(elapsedValue, stepCountValue, positionValue, velocityValue,
                speedValue, accelerationValue, radiusValue, altitudeValue, targetDistanceValue,
                relativeSpeedValue, energyValue, periodValue, lastDeltaVValue)) label.setText("--");
        phaseValue.setText("--");
    }

    private void setButtonsInitialized(boolean initialized)
    {
        runButton.setDisable(!initialized);
        pauseButton.setDisable(!initialized);
        resetButton.setDisable(!initialized);
    }

    private void setStatus(String message, String color)
    {
        topStatus.setText(message);
        topStatus.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
    }

    private VBox panelBox(double spacing)
    {
        VBox box = new VBox(spacing);
        box.setStyle("-fx-background-color: #0C1A2B; -fx-background-radius: 12; -fx-border-color: #1B3552; -fx-border-radius: 12;");
        return box;
    }

    private VBox fieldGroup(String title, javafx.scene.Node field)
    {
        Label label = new Label(title);
        label.setStyle("-fx-text-fill: #A9BED5; -fx-font-size: 10px;");
        if (field == initialAltitudeField) initialAltitudeCaption = label;
        if (field == targetAltitudeField) targetAltitudeCaption = label;
        return new VBox(4, label, field);
    }

    private HBox resultRow(String title, Label value)
    {
        Label name = new Label(title);
        name.setStyle("-fx-text-fill: #8EA7C5; -fx-font-size: 9px;");
        value.setWrapText(true);
        value.setStyle("-fx-text-fill: #E5F4FF; -fx-font-size: 9px; -fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(6, name, spacer, value);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Label telemetryName(String text)
    {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #8EA7C5; -fx-font-size: 9px;");
        label.setWrapText(true);
        return label;
    }

    private Label telemetryValue(Label label)
    {
        label.setStyle("-fx-text-fill: #E5F4FF; -fx-font-size: 9px; -fx-font-family: 'Consolas';");
        label.setWrapText(true);
        label.setMaxWidth(170);
        return label;
    }

    private Label sectionHeading(String text)
    {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #D7E8FA; -fx-font-size: 11px; -fx-font-weight: bold;");
        return label;
    }

    private Label smallHeading(String text)
    {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #6484A6; -fx-font-size: 9px; -fx-font-weight: bold;");
        return label;
    }

    private Label legend(String text, String color, boolean dashed)
    {
        Label label = new Label((dashed ? "┄ " : "━ ") + text);
        label.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 9px; -fx-font-weight: bold;");
        return label;
    }

    private Region separator()
    {
        Region line = new Region();
        line.setMinHeight(1); line.setPrefHeight(1); line.setMaxHeight(1);
        line.setStyle("-fx-background-color: #203750;");
        return line;
    }

    private void styleTextField(TextField field)
    {
        field.setPrefHeight(32);
        field.setStyle("-fx-background-color: #07111F; -fx-text-fill: #E7F0FC; -fx-prompt-text-fill: #607B9B; -fx-control-inner-background: #07111F; -fx-border-color: #29435F; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 6 9 6 9;");
    }

    private void styleButton(Button button, String background, String foreground)
    {
        button.setStyle("-fx-background-color: " + background + "; -fx-text-fill: " + foreground + "; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 10 14 10 14; -fx-background-radius: 6; -fx-cursor: hand;");
        button.setMaxWidth(Double.MAX_VALUE);
    }

    private double readNumber(TextField field, String fieldName)
    {
        try
        {
            double value = Double.parseDouble(field.getText().trim());
            if (!Double.isFinite(value)) throw new NumberFormatException();
            return value;
        }
        catch (NumberFormatException ex)
        {
            throw new NumberFormatException(fieldName + " must be a finite number.");
        }
    }

    private String format(double value, String unit)
    {
        return !Double.isFinite(value) ? "N/A" : String.format(Locale.US, "%.3f %s", value, unit);
    }

    private String formatNumber(double value)
    {
        return String.format(Locale.US, "%.3g", value);
    }

    private String formatScientific(double value)
    {
        return Double.isFinite(value) ? String.format(Locale.US, "%.4e", value) : "N/A";
    }

    private String formatDistance(double metres)
    {
        if (!Double.isFinite(metres)) return "N/A";
        double abs = Math.abs(metres);
        if (abs >= 0.1 * Constants.AU_TO_M) return String.format(Locale.US, "%.5f AU", metres / Constants.AU_TO_M);
        if (abs >= 100000.0) return String.format(Locale.US, "%.3f km", metres / 1000.0);
        return String.format(Locale.US, "%.3f m", metres);
    }

    private String formatVector(Vector3 vector)
    {
        if (vector == null) return "N/A";
        return String.format(Locale.US, "X %s\nY %s\nZ %s", formatScientific(vector.getX()),
            formatScientific(vector.getY()), formatScientific(vector.getZ()));
    }

    private String formatDuration(double seconds)
    {
        if (!Double.isFinite(seconds) || seconds < 0.0) return "N/A";
        long wholeSeconds = (long) Math.floor(seconds);
        long days = wholeSeconds / 86400L;
        long hours = (wholeSeconds % 86400L) / 3600L;
        long minutes = (wholeSeconds % 3600L) / 60L;
        long secs = wholeSeconds % 60L;
        if (days > 0) return String.format(Locale.US, "%dd %02dh %02dm", days, hours, minutes);
        if (hours > 0) return String.format(Locale.US, "%02dh %02dm %02ds", hours, minutes, secs);
        if (minutes > 0) return String.format(Locale.US, "%02dm %02ds", minutes, secs);
        return String.format(Locale.US, "%.2f s", seconds);
    }

    private String formatSpeedScale(double scale)
    {
        if (scale >= 1000.0) return String.format(Locale.US, "%,.0fx", scale);
        return String.format(Locale.US, "%.0fx", scale);
    }

    private Double parseSpeedScale(String value)
    {
        return Double.valueOf(value.replace("x", "").replace(",", "").trim());
    }

    private void showError(String message)
    {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("ForgeOrbit V5");
        alert.setHeaderText("Mission or simulation error");
        alert.setContentText(message == null || message.trim().isEmpty() ? "An unspecified error occurred." : message);
        alert.showAndWait();
    }

    public Mission getCurrentMission() { return currentMission; }
    public TransferResult getCurrentTransferResult() { return currentTransferResult; }
    public ForgeOrbitTimeStepEngine getActiveEngine() { return activeEngine; }

    public void dispose()
    {
        simulationPlaying = false;
        animationTimer.stop();
        if (activeEngine != null) activeEngine.stop();
    }
}
