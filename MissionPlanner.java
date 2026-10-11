public class MissionPlanner
{
    public MissionPlanResult createPlan(MissionRequest request)
    {
        if (request == null)
            throw new IllegalArgumentException("Mission request cannot be null.");

        switch (request.getMissionType())
        {
            case EARTH_ORBIT:
                return planEarthOrbit(request);
            case LUNAR:
                return planLunar(request);
            case INTERPLANETARY:
                return planInterplanetary(request);
            default:
                throw new IllegalArgumentException("Unsupported mission type.");
        }
    }

    private MissionPlanResult planEarthOrbit(MissionRequest request)
    {
        Orbit initial = new Orbit(request.getLaunchAltitudeMetres());
        Orbit target = new Orbit(request.getTargetAltitudeMetres());
        TransferResult transfer = new HohmannTransfer().calculateTransfer(initial, target);
        return new MissionPlanResult(request.getMissionName(), request.getMissionType(),
            "Ideal Hohmann transfer between circular Earth orbits.",
            transfer.getTotalDeltaV(), transfer.getTransferTime(), true);
    }

    private MissionPlanResult planLunar(MissionRequest request)
    {
        double parkingAltitude = request.getLaunchAltitudeMetres() > 0.0
            ? request.getLaunchAltitudeMetres() : 200000.0;
        LunarMissionResult result = new LunarMissionCalculator().calculateTransfer(parkingAltitude);
        return new MissionPlanResult(request.getMissionName(), request.getMissionType(),
            "Simplified patched-conic estimate from low Earth orbit to a 100 km lunar orbit.",
            result.getTotalDeltaV(), result.getTransferTimeSeconds(), true);
    }

    private MissionPlanResult planInterplanetary(MissionRequest request)
    {
        InterplanetaryTransferResult result = new InterplanetaryTransfer().calculateTransfer(
            request.getOriginBodyName(), request.getDestinationBodyName());
        return new MissionPlanResult(request.getMissionName(), request.getMissionType(),
            "Ideal coplanar Hohmann estimate from " + result.getOriginBodyName()
                + " to " + result.getDestinationBodyName() + ". Launch windows and capture are not modelled.",
            result.getTotalDeltaV(), result.getTransferTimeSeconds(), true);
    }
}
