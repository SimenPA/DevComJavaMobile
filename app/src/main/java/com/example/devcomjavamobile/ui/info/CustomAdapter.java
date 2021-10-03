package com.example.devcomjavamobile.ui.info;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.example.devcomjavamobile.R;
import com.example.devcomjavamobile.Utility;

public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.ViewHolder> {

    private String[] devices;
    private String[] communities;
    private Activity activity;

    private final String TAG = CustomAdapter.class.getSimpleName();

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;
        private final TextView connectedStatusTextView;
        private final TextView communityTextView;

        private final CardView cardView;


        public ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View

            textView = (TextView) view.findViewById(R.id.textView);
            connectedStatusTextView = (TextView) view.findViewById(R.id.connectedStatusTextView);
            communityTextView = (TextView) view.findViewById(R.id.communityTextView);
            cardView = (CardView) view.findViewById(R.id.cardView);
        }

        public TextView getTextView() {
            return textView;
        }
        public TextView getConnectedStatusTextView() { return connectedStatusTextView; }
        public TextView getCommunityTextView() { return communityTextView; }

        public CardView getCardView() { return cardView; }
    }

    /**
     * Initialize the dataset of the Adapter.
     *
     * @param devices String[] containing the data to populate views to be used
     * by RecyclerView.
     */
    public CustomAdapter(String[] devices, String[] communities, Activity activity) {
        this.devices = devices;
        this.communities = communities;
        this.activity =  activity;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view, which defines the UI of the list item
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.card_view_item, viewGroup, false);

        return new ViewHolder(view);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        viewHolder.getTextView().setText(devices[position]);
        viewHolder.getCommunityTextView().setText(communities[position]);
        viewHolder.getConnectedStatusTextView().setText("DISCONNECTED");

        viewHolder.getCardView().setOnClickListener(view -> {
            String ipv6Address = Utility.generateIpv6Address(viewHolder.getCommunityTextView().getText().toString(), viewHolder.getTextView().getText().toString());
            NavController navController = Navigation.findNavController(view);
            navController.navigate(R.id.navigation_transport);
            Log.i(TAG, "IPv6 address: " + ipv6Address);
            activity.getIntent().putExtra(" ", ipv6Address);

        });
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return devices.length;
    }
}
