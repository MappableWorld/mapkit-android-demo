package world.mappable.mapkitdemo;

import static world.mappable.mapkitdemo.ConstantsUtils.CLUSTER_CENTERS;

import android.app.Activity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.widget.Toast;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.FontMetrics;
import android.graphics.Paint.Style;

import world.mappable.mapkit.MapKitFactory;
import world.mappable.mapkit.geometry.Point;
import world.mappable.mapkit.map.CameraPosition;
import world.mappable.mapkit.map.Cluster;
import world.mappable.mapkit.map.ClusterListener;
import world.mappable.mapkit.map.ClusterTapListener;
import world.mappable.mapkit.map.IconStyle;
import world.mappable.mapkit.map.ClusterizedPlacemarkCollection;
import world.mappable.mapkit.mapview.MapView;
import world.mappable.runtime.image.ImageProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * This example shows how to add a collection of clusterized placemarks to the map.
 */
public class ClusteringActivity extends Activity implements ClusterListener, ClusterTapListener {
    private MapView mapView;
    private static final float FONT_SIZE = 15;
    private static final float MARGIN_SIZE = 3;
    private static final float STROKE_SIZE = 3;
    private static final int PLACEMARKS_NUMBER = 2000;

    public class TextImageProvider extends ImageProvider {
        @Override
        public String getId() {
            return "text_" + text;
        }

        private final String text;

        @Override
        public Bitmap getImage() {
            DisplayMetrics metrics = getResources().getDisplayMetrics();

            Paint textPaint = new Paint();
            textPaint.setTextSize(FONT_SIZE * metrics.density);
            textPaint.setTextAlign(Align.CENTER);
            textPaint.setStyle(Style.FILL);
            textPaint.setAntiAlias(true);

            float widthF = textPaint.measureText(text);
            FontMetrics textMetrics = textPaint.getFontMetrics();
            float heightF = Math.abs(textMetrics.bottom) + Math.abs(textMetrics.top);
            float textRadius = (float) Math.sqrt(widthF * widthF + heightF * heightF) / 2;
            float internalRadius = textRadius + MARGIN_SIZE * metrics.density;
            float externalRadius = internalRadius + STROKE_SIZE * metrics.density;

            int width = (int) (2 * externalRadius + 0.5);

            Bitmap bitmap = Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);

            Paint backgroundPaint = new Paint();
            backgroundPaint.setAntiAlias(true);
            backgroundPaint.setColor(Color.RED);
            canvas.drawCircle(width / 2, width / 2, externalRadius, backgroundPaint);

            backgroundPaint.setColor(Color.WHITE);
            canvas.drawCircle(width / 2, width / 2, internalRadius, backgroundPaint);

            canvas.drawText(
                    text,
                    width / 2,
                    width / 2 - (textMetrics.ascent + textMetrics.descent) / 2,
                    textPaint);

            return bitmap;
        }

        public TextImageProvider(String text) {
            this.text = text;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.clustering);
        super.onCreate(savedInstanceState);

        mapView = findViewById(R.id.mapview);
        mapView.getMapWindow().getMap().move(new CameraPosition(
                CLUSTER_CENTERS.get(0), 3, 0, 0));
        ImageProvider imageProvider = ImageProvider.fromResource(
                ClusteringActivity.this, R.drawable.search_result);

        // Note that application must retain strong references to both
        // cluster listener and cluster tap listener
        ClusterizedPlacemarkCollection clusterizedCollection =
                mapView.getMapWindow().getMap().getMapObjects().addClusterizedPlacemarkCollection(this);

        List<Point> points = createPoints(PLACEMARKS_NUMBER);
        clusterizedCollection.addPlacemarks(points, imageProvider, new IconStyle());

        // Placemarks won't be displayed until this method is called. It must be also called
        // to force clusters update after collection change
        clusterizedCollection.clusterPlacemarks(60, 15);
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
    public void onClusterAdded(Cluster cluster) {
        // We setup cluster appearance and tap handler in this method
        cluster.getAppearance().setIcon(
                new TextImageProvider(Integer.toString(cluster.getSize())));
        cluster.addClusterTapListener(this);
    }

    @Override
    public boolean onClusterTap(Cluster cluster) {
        Toast.makeText(
                getApplicationContext(),
                String.format(getString(R.string.cluster_tap_message), cluster.getSize()),
                Toast.LENGTH_SHORT).show();

        // We return true to notify map that the tap was handled and shouldn't be
        // propagated further.
        return true;
    }

    private List<Point> createPoints(int count) {
        ArrayList<Point> points = new ArrayList<Point>();
        Random random = new Random();

        for (int i = 0; i < count; ++i) {
            Point clusterCenter = CLUSTER_CENTERS.get(random.nextInt(CLUSTER_CENTERS.size()));
            double latitude = clusterCenter.getLatitude() + Math.random() - 0.5;
            double longitude = clusterCenter.getLongitude() + Math.random() - 0.5;

            points.add(new Point(latitude, longitude));
        }

        return points;
    }
}
