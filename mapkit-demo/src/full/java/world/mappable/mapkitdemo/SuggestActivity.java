package world.mappable.mapkitdemo;

import static world.mappable.mapkitdemo.ConstantsUtils.DEFAULT_POINT;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import world.mappable.mapkit.MapKitFactory;
import world.mappable.mapkit.geometry.BoundingBox;
import world.mappable.mapkit.geometry.Point;
import world.mappable.mapkit.search.SearchFactory;
import world.mappable.mapkit.search.SearchManager;
import world.mappable.mapkit.search.SearchManagerType;
import world.mappable.mapkit.search.SuggestOptions;
import world.mappable.mapkit.search.SuggestResponse;
import world.mappable.mapkit.search.SuggestSession;
import world.mappable.mapkit.search.SuggestType;
import world.mappable.runtime.Error;
import world.mappable.runtime.network.NetworkError;
import world.mappable.runtime.network.RemoteError;

import java.util.ArrayList;
import java.util.List;

/**
 * This example shows how to request a suggest for search requests.
 */
public class SuggestActivity extends Activity implements SuggestSession.SuggestListener {
    private final int RESULT_NUMBER_LIMIT = 5;

    private SearchManager searchManager;
    private SuggestSession suggestSession;
    private ListView suggestResultView;
    private ArrayAdapter resultAdapter;
    private List<String> suggestResult;

    private final double BOX_SIZE = 0.2;
    private final BoundingBox BOUNDING_BOX = new BoundingBox(
        new Point(DEFAULT_POINT.getLatitude() - BOX_SIZE, DEFAULT_POINT.getLongitude() - BOX_SIZE),
        new Point(DEFAULT_POINT.getLatitude() + BOX_SIZE, DEFAULT_POINT.getLongitude() + BOX_SIZE));
    private final SuggestOptions SEARCH_OPTIONS =  new SuggestOptions().setSuggestTypes(
        SuggestType.GEO.value |
        SuggestType.BIZ.value |
        SuggestType.TRANSIT.value);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SearchFactory.initialize(this);
        setContentView(R.layout.suggest);
        super.onCreate(savedInstanceState);

        searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED);
        suggestSession = searchManager.createSuggestSession();
        EditText queryEdit = (EditText)findViewById(R.id.suggest_query);
        suggestResultView = (ListView)findViewById(R.id.suggest_result);
        suggestResult = new ArrayList<>();
        resultAdapter = new ArrayAdapter(this,
                android.R.layout.simple_list_item_2,
                android.R.id.text1,
                suggestResult);
        suggestResultView.setAdapter(resultAdapter);

        queryEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                requestSuggest(editable.toString());
            }
        });
    }

    @Override
    protected void onStop() {
        MapKitFactory.getInstance().onStop();
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        MapKitFactory.getInstance().onStart();
    }

    @Override
    public void onResponse(@NonNull SuggestResponse suggest) {
        suggestResult.clear();
        for (int i = 0; i < Math.min(RESULT_NUMBER_LIMIT, suggest.getItems().size()); i++) {
            suggestResult.add(suggest.getItems().get(i).getDisplayText());
        }
        resultAdapter.notifyDataSetChanged();
        suggestResultView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onError(@NonNull Error error) {
        String errorMessage = getString(R.string.unknown_error_message);
        if (error instanceof RemoteError) {
            errorMessage = getString(R.string.remote_error_message);
        } else if (error instanceof NetworkError) {
            errorMessage = getString(R.string.network_error_message);
        }

        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
    }

    private void requestSuggest(String query) {
        suggestResultView.setVisibility(View.INVISIBLE);
        suggestSession.suggest(query, BOUNDING_BOX, SEARCH_OPTIONS, this);
    }
}
