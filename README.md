
# ForgeOrbit 

### Orbital Mechanics, Spacecraft Trajectory Simulation & Mission Analysis

ForgeOrbit is a Java-based orbital mechanics and mission simulation application built with JavaFX. It models spacecraft trajectories, calculates orbital transfers, and provides mission telemetry and analysis through an interactive graphical interface.

## Features

- **Earth Orbit Simulation** — Explore spacecraft motion around Earth and different orbital configurations.
- **Lunar Missions** — Calculate lunar transfers, with orbital capture as the mission objective.
- **Interplanetary Transfers** — Calculate transfers between planetary bodies.
- **Numerical Time-Step Simulation** — Advance spacecraft motion through simulation time steps.
- **Trajectory Visualisation** — Display spacecraft trajectories alongside celestial bodies.
- **Live Mission Telemetry** — Inspect simulation time, spacecraft state, distance to target, and other available mission values.
- **Mission Analysis** — Review calculated mission parameters and feasibility.
- **Interactive Dashboard** — Configure missions, control simulation playback, and inspect results.
- **Celestial Body Models** — Represent Earth, satellites, and other solar system bodies.

## Technology Stack

- Java
- JavaFX
- Object-oriented programming
- Orbital mechanics and physics-based calculations
- Numerical spacecraft simulation

## Project Structure — Explained in Batches

The source files are grouped by responsibility to make the architecture easier to understand.

### Batch 1 — Application, Interface and Simulation Runtime

| File | What it does |
|---|---|
| `Main.java` | Starts the JavaFX application, creates the main window, and loads the dashboard. |
| `MissionDashboard.java` | Provides the main interface, mission settings, simulation controls, telemetry, and analysis view. |
| `ForgeOrbitTimeStepEngine.java` | Advances spacecraft simulation using numerical time steps and manages simulated trajectory states. |
| `OrbitCanvas.java` | Draws orbital paths and spacecraft-related visual elements. |
| `Simulation.java` | Handles the legacy simulation and its associated animation or rendering workflow. |

**Batch purpose:** These files connect the user interface to the simulation and visualisation layers.

### Batch 2 — Celestial Bodies and Physical Constants

| File | What it does |
|---|---|
| `CelestialBody.java` | Defines the common properties of celestial bodies. |
| `Earth.java` | Represents Earth and its relevant physical properties. |
| `Satellite.java` | Represents a spacecraft or satellite and its orbital state. |
| `SolarSystemCatalog.java` | Stores or provides access to solar system body data. |
| `Constants.java` | Centralises physical and simulation constants. |

**Batch purpose:** These classes establish the bodies and physical values used throughout the simulation.

### Batch 3 — Core Orbital Mechanics

| File | What it does |
|---|---|
| `Vector3.java` | Provides three-dimensional vector calculations. |
| `Orbit.java` | Represents an orbit and its associated parameters. |
| `OrbitalElements.java` | Stores parameters used to describe an orbit. |
| `OrbitalMechanics.java` | Performs foundational orbital mechanics calculations. |
| `PlanetaryState.java` | Represents the calculated state of a planetary body. |

**Batch purpose:** These classes provide the mathematical foundation for representing and calculating motion in space.

### Batch 4 — Planetary Positions and Transfer Calculations

| File | What it does |
|---|---|
| `PlanetaryEphemeris.java` | Calculates or provides planetary positions for selected simulation times. |
| `HohmannTransfer.java` | Calculates the parameters of a Hohmann orbital transfer. |
| `TransferResult.java` | Stores the results of a transfer calculation. |
| `InterplanetaryTransfer.java` | Handles interplanetary transfer calculations. |
| `InterplanetaryTransferResult.java` | Stores the calculated results of an interplanetary transfer. |

**Batch purpose:** These classes calculate planetary positions and transfer parameters needed to plan spacecraft journeys.

### Batch 5 — Lunar Missions, Propulsion and Analysis

| File | What it does |
|---|---|
| `LunarMissionCalculator.java` | Calculates parameters for missions involving the Moon. |
| `LunarMissionResult.java` | Stores calculated lunar mission results. |
| `Propulsion.java` | Represents propulsion-related parameters and calculations. |
| `PropellantCalculator.java` | Estimates propellant requirements from the model's inputs and assumptions. |
| `MissionAnalysis.java` | Evaluates calculated mission parameters and presents analytical results. |

**Batch purpose:** These classes connect mission calculations with propulsion requirements and engineering analysis.

### Batch 6 — Mission Planning and Results

| File | What it does |
|---|---|
| `Mission.java` | Represents a mission and its associated information. |
| `MissionPlanner.java` | Coordinates mission planning calculations. |
| `MissionRequest.java` | Holds the inputs and settings supplied for a mission. |
| `MissionPlanResult.java` | Stores the outcome of mission planning. |
| `MissionResult.java` | Represents the results produced by a mission calculation or execution. |

**Batch purpose:** These classes organise mission inputs, coordinate calculations, and return results to the application.

### Batch 7 — Supporting Validation and Mission Types

| File | What it does |
|---|---|
| `MissionType.java` | Defines the supported mission categories used by the planning system. |
| `InputValidator.java` | Checks user inputs before they are used in calculations. |

**Batch purpose:** These supporting classes help the application identify mission modes and validate inputs.

## Requirements

- A compatible Java Development Kit (JDK)
- JavaFX SDK containing the `javafx.controls` module
- Windows or another environment configured with a compatible JavaFX runtime

## Running the Application

Open a terminal in the project directory.

**Compile:**

```powershell
javac -encoding UTF-8 --module-path "PATH_TO_FX" --add-modules javafx.controls *.java
```

**Launch:**

```powershell
java --module-path "PATH_TO_FX" --add-modules javafx.controls Main
```

Replace `PATH_TO_FX` with the location of the JavaFX library directory.

## Engineering Scope

ForgeOrbit is an educational and engineering-oriented software project combining orbital mechanics, transfer calculations, numerical propagation, trajectory visualisation, propulsion estimates, and mission analysis.

Its results depend on the physical models, numerical integration method, time-step selection, initial conditions, and assumptions used. The application is intended for simulation and learning; its outputs should not be treated as validation of real-world flight readiness.

## Author

**AadityaK24**

Developed as an independent aerospace and orbital simulation software project.
