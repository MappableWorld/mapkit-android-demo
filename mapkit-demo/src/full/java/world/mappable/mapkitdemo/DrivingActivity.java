package world.mappable.mapkitdemo;


import static world.mappable.mapkitdemo.ConstantsUtils.DRIVING_ROUTE_END_LOCATION;
import static world.mappable.mapkitdemo.ConstantsUtils.DRIVING_ROUTE_START_LOCATION;

import android.os.Bundle;
import android.app.Activity;
import android.widget.Toast;

import java.util.List;
import java.util.ArrayList;

import world.mappable.mapkit.directions.DirectionsFactory;
import world.mappable.mapkit.directions.driving.DrivingOptions;
import world.mappable.mapkit.directions.driving.DrivingRoute;
import world.mappable.mapkit.directions.driving.DrivingRouter;
import world.mappable.mapkit.directions.driving.DrivingSession;
import world.mappable.mapkit.directions.driving.VehicleOptions;
import world.mappable.mapkit.geometry.Point;
import world.mappable.mapkit.map.CameraPosition;
import world.mappable.mapkit.map.MapObjectCollection;
import world.mappable.mapkit.MapKitFactory;
import world.mappable.mapkit.mapview.MapView;
import world.mappable.mapkit.RequestPoint;
import world.mappable.mapkit.RequestPointType;
import world.mappable.runtime.Error;
import world.mappable.runtime.network.NetworkError;
import world.mappable.runtime.network.RemoteError;

/**
 * This example shows how to build routes between two points and display them on the map.
 * Note: Routing API calls count towards MapKit daily usage limits.
 */
public class DrivingActivity extends Activity implements DrivingSession.DrivingRouteListener {
    private final Point SCREEN_CENTER = new Point(
            (DRIVING_ROUTE_START_LOCATION.getLatitude() + DRIVING_ROUTE_END_LOCATION.getLatitude()) / 2,
            (DRIVING_ROUTE_START_LOCATION.getLongitude() + DRIVING_ROUTE_END_LOCATION.getLongitude()) / 2);

    private MapView mapView;
    private MapObjectCollection mapObjects;
    private DrivingRouter drivingRouter;
    private DrivingSession drivingSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DirectionsFactory.initialize(this);

        setContentView(R.layout.driving);
        super.onCreate(savedInstanceState);

        mapView = findViewById(R.id.mapview);
        mapView.getMap().move(new CameraPosition(
                SCREEN_CENTER, 5, 0, 0));
        drivingRouter = DirectionsFactory.getInstance().createDrivingRouter();
        mapObjects = mapView.getMap().getMapObjects().addCollection();

        submitRequest();
    }

    @Override
    protected void onStop() {
        mapView.onStop();
        MapKitFactory.getInstance().onStop();
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        MapKitFactory.getInstance().onStart();
        mapView.onStart();
    }

    @Override
    public void onDrivingRoutes(List<DrivingRoute> routes) {
        for (DrivingRoute route : routes) {
            mapObjects.addPolyline(route.getGeometry());
        }
    }

    @Override
    public void onDrivingRoutesError(Error error) {
        String errorMessage = getString(R.string.unknown_error_message);
        if (error instanceof RemoteError) {
            errorMessage = getString(R.string.remote_error_message);
        } else if (error instanceof NetworkError) {
            errorMessage = getString(R.string.network_error_message);
        }

        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
    }

    private void submitRequest() {
        DrivingOptions drivingOptions = new DrivingOptions();
        VehicleOptions vehicleOptions = new VehicleOptions();
        ArrayList<RequestPoint> requestPoints = new ArrayList<>();
        requestPoints.add(new RequestPoint(
                DRIVING_ROUTE_START_LOCATION,
                RequestPointType.WAYPOINT,
                null,
                null));
        requestPoints.add(new RequestPoint(
                DRIVING_ROUTE_START_LOCATION,
                RequestPointType.WAYPOINT,
                null,
                null));
        drivingSession = drivingRouter.requestRoutes(requestPoints, drivingOptions, vehicleOptions, this);
    }
}
