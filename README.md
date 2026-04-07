# Airport Simulation System

A discrete-event airport simulation system designed to model aircraft arrivals, departures, and runway allocation under varying operational conditions.

## Overview
This project simulates real-world airport operations to evaluate throughput, delays, and system performance under different runway configurations and disruptions.

## Key Features
- Discrete-event simulation engine for scheduling aircraft events
- Multi-threaded architecture separating simulation and UI logic
- Runway allocation and queue management system
- Real-time tracking of aircraft states and event execution
- Configurable parameters for testing different airport scenarios

## Technical Implementation
- Built in **Java** using object-oriented design principles
- Implemented custom event scheduling using priority-based structures
- Applied **Command Pattern** for simulation events and state transitions
- Designed modular architecture with clear separation of concerns
- Used **JavaFX** for GUI and **multi-threading** for concurrent execution

## Project Structure
- `src/` – Core simulation logic and system components
- `pom.xml` – Maven configuration and dependencies
- `User Guide.pdf` – Detailed instructions and system usage

## Future Improvements
- Enhanced UI visualisation of aircraft movements
- Advanced analytics for performance metrics
- Support for multi-airport simulations
