package world.mappable.mapkitdemo;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import world.mappable.mapkit.MapKitFactory;
import world.mappable.mapkit.geometry.Circle;
import world.mappable.mapkit.geometry.LinearRing;
import world.mappable.mapkit.geometry.Point;
import world.mappable.mapkit.geometry.Polygon;
import world.mappable.mapkit.geometry.Polyline;
import world.mappable.mapkit.map.CameraPosition;
import world.mappable.mapkit.map.CircleMapObject;
import world.mappable.mapkit.map.IconStyle;
import world.mappable.mapkit.map.MapObject;
import world.mappable.mapkit.map.MapObjectCollection;
import world.mappable.mapkit.map.MapObjectTapListener;
import world.mappable.mapkit.map.PlacemarkMapObject;
import world.mappable.mapkit.map.PolygonMapObject;
import world.mappable.mapkit.map.PolylineMapObject;
import world.mappable.mapkit.mapview.MapView;
import world.mappable.runtime.image.AnimatedImageProvider;
import world.mappable.runtime.image.ImageProvider;
import world.mappable.runtime.ui_view.ViewProvider;

import java.util.ArrayList;
import java.util.Random;

/**
 * This example shows how to add simple objects such as polygons, circles and polylines to the map.
 * It also shows how to display images instead.
 */
public class MapObjectsActivity extends Activity {
    private final Point CAMERA_TARGET = new Point(25.229, 55.289);
    private final Point ANIMATED_RECTANGLE_CENTER = new Point(25.234, 55.294);
    private final Point TRIANGLE_CENTER = new Point(25.224, 55.284);
    private final Point POLYLINE_CENTER = CAMERA_TARGET;
    private final Point CIRCLE_CENTER = new Point(25.235, 55.289);
    private final Point DRAGGABLE_PLACEMARK_CENTER = new Point(25.224, 55.289);
    private final Point ANIMATED_PLACEMARK_CENTER = new Point(25.229, 55.300);
    private final double OBJECT_SIZE = 0.0015;

    private MapView mapView;
    private MapObjectCollection mapObjects;
    private Handler animationHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.map_objects);
        super.onCreate(savedInstanceState);
        mapView = findViewById(R.id.mapview);
        mapView.getMap().move(
                new CameraPosition(CAMERA_TARGET, 15.0f, 0.0f, 0.0f));
        mapObjects = mapView.getMap().getMapObjects().addCollection();
        animationHandler = new Handler();
        createMapObjects();
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

    private void createMapObjects() {
        AnimatedImageProvider animatedImage = AnimatedImageProvider.fromAsset(this, "animation.png");
        ArrayList<Point> rectPoints = new ArrayList<>();
        rectPoints.add(new Point(
                ANIMATED_RECTANGLE_CENTER.getLatitude() - OBJECT_SIZE,
                ANIMATED_RECTANGLE_CENTER.getLongitude() - OBJECT_SIZE));
        rectPoints.add(new Point(
                ANIMATED_RECTANGLE_CENTER.getLatitude() - OBJECT_SIZE,
                ANIMATED_RECTANGLE_CENTER.getLongitude() + OBJECT_SIZE));
        rectPoints.add(new Point(
                ANIMATED_RECTANGLE_CENTER.getLatitude() + OBJECT_SIZE,
                ANIMATED_RECTANGLE_CENTER.getLongitude() + OBJECT_SIZE));
        rectPoints.add(new Point(
                ANIMATED_RECTANGLE_CENTER.getLatitude() + OBJECT_SIZE,
                ANIMATED_RECTANGLE_CENTER.getLongitude() - OBJECT_SIZE));
        PolygonMapObject rect = mapObjects.addPolygon(
                new Polygon(new LinearRing(rectPoints), new ArrayList<LinearRing>()));
        rect.setStrokeColor(Color.TRANSPARENT);
        rect.setFillColor(Color.TRANSPARENT);
        rect.setAnimatedImage(animatedImage, 32.0f);

        ArrayList<Point> trianglePoints = new ArrayList<>();
        trianglePoints.add(new Point(
                TRIANGLE_CENTER.getLatitude() + OBJECT_SIZE,
                TRIANGLE_CENTER.getLongitude() - OBJECT_SIZE));
        trianglePoints.add(new Point(
                TRIANGLE_CENTER.getLatitude() - OBJECT_SIZE,
                TRIANGLE_CENTER.getLongitude() - OBJECT_SIZE));
        trianglePoints.add(new Point(
                TRIANGLE_CENTER.getLatitude(),
                TRIANGLE_CENTER.getLongitude() + OBJECT_SIZE));
        PolygonMapObject triangle = mapObjects.addPolygon(
                new Polygon(new LinearRing(trianglePoints), new ArrayList<LinearRing>()));
        triangle.setFillColor(Color.BLUE);
        triangle.setStrokeColor(Color.BLACK);
        triangle.setStrokeWidth(1.0f);
        triangle.setZIndex(100.0f);

        createTappableCircle();

        ArrayList<Point> polylinePoints = new ArrayList<>();
        polylinePoints.add(new Point(
                POLYLINE_CENTER.getLatitude() + OBJECT_SIZE,
                POLYLINE_CENTER.getLongitude()- OBJECT_SIZE));
        polylinePoints.add(new Point(
                POLYLINE_CENTER.getLatitude() - OBJECT_SIZE,
                POLYLINE_CENTER.getLongitude()- OBJECT_SIZE));
        polylinePoints.add(new Point(
                POLYLINE_CENTER.getLatitude(),
                POLYLINE_CENTER.getLongitude() + OBJECT_SIZE));

        PolylineMapObject polyline = mapObjects.addPolyline(new Polyline(polylinePoints));
        polyline.setStrokeColor(Color.BLACK);
        polyline.setZIndex(100.0f);

        PlacemarkMapObject mark = mapObjects.addPlacemark(DRAGGABLE_PLACEMARK_CENTER);
        mark.setOpacity(0.5f);
        mark.setIcon(ImageProvider.fromResource(this, R.drawable.mark));
        mark.setDraggable(true);

        createPlacemarkMapObjectWithViewProvider();
        createAnimatedPlacemark();
    }

    // Strong reference to the listener.
    private MapObjectTapListener circleMapObjectTapListener = new MapObjectTapListener() {
        @Override
        public boolean onMapObjectTap(MapObject mapObject, Point point) {
            if (mapObject instanceof CircleMapObject) {
                CircleMapObject circle = (CircleMapObject)mapObject;

                float randomRadius = 100.0f + 50.0f * new Random().nextFloat();

                Circle curGeometry = circle.getGeometry();
                Circle newGeometry = new Circle(curGeometry.getCenter(), randomRadius);
                circle.setGeometry(newGeometry);

                Object userData = circle.getUserData();
                if (userData instanceof CircleMapObjectUserData) {
                    CircleMapObjectUserData circleUserData = (CircleMapObjectUserData)userData;

                    Toast toast = Toast.makeText(
                            getApplicationContext(),
                            "Circle with id " + circleUserData.id + " and description '"
                                    + circleUserData.description + "' tapped",
                            Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
            return true;
        }
    };

    private class CircleMapObjectUserData {
        final int id;
        final String description;

        CircleMapObjectUserData(int id, String description) {
            this.id = id;
            this.description = description;
        }
    }

    private void createTappableCircle() {
        CircleMapObject circle = mapObjects.addCircle(
                new Circle(CIRCLE_CENTER, 100), Color.GREEN, 2, Color.RED);
        circle.setZIndex(100.0f);
        circle.setUserData(new CircleMapObjectUserData(42, "Tappable circle"));

        // Client code must retain strong reference to the listener.
        circle.addTapListener(circleMapObjectTapListener);
    }

    private void createPlacemarkMapObjectWithViewProvider() {
        final TextView textView = new TextView(this);
        final int[] colors = new int[] { Color.RED, Color.GREEN, Color.BLACK };
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        textView.setLayoutParams(params);

        textView.setTextColor(Color.RED);
        textView.setText("Hello, World!");

        final ViewProvider viewProvider = new ViewProvider(textView);
        final PlacemarkMapObject viewPlacemark =
                mapObjects.addPlacemark(new Point(59.946263, 30.315181), viewProvider);

        final Random random = new Random();
        final int delayToShowInitialText = 5000;  // milliseconds
        final int delayToShowRandomText = 500; // milliseconds;

        // Show initial text `delayToShowInitialText` milliseconds and then
        // randomly change text in textView every `delayToShowRandomText` milliseconds
        animationHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                final int randomInt = random.nextInt(1000);
                textView.setText("Some text version " + randomInt);
                textView.setTextColor(colors[randomInt % colors.length]);
                viewProvider.snapshot();
                viewPlacemark.setView(viewProvider);
                animationHandler.postDelayed(this, delayToShowRandomText);
            }
        }, delayToShowInitialText);
    }

    private void createAnimatedPlacemark() {
        AnimatedImageProvider imageProvider =
                AnimatedImageProvider.fromAsset(this,"animation.png");
        PlacemarkMapObject animatedPlacemark =
                mapObjects.addPlacemark(ANIMATED_PLACEMARK_CENTER, imageProvider, new IconStyle());
        animatedPlacemark.useAnimation().play();
    }
}
