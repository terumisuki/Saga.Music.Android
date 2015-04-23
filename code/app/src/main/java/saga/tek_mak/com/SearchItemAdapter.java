package saga.tek_mak.com;

import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class SearchItemAdapter extends ArrayAdapter<SearchItem> {
    Context context;
    int layoutResourceId;
    List<SearchItem> data = null;

    public SearchItemAdapter(Context context, int textViewResourceId, List<SearchItem> searchItems) {
        super(context, textViewResourceId, searchItems);
        this.context = context;
        this.layoutResourceId = textViewResourceId;
        this.data = searchItems;
    }



    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View row = convertView;
        final SearchItemHolder holder;

        final SearchItem searchItem = data.get(position);

        if(row == null)
        {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);

            holder = new SearchItemHolder();
            row.setTag(holder);


            holder.txtItemTypeLabel = (TextView)row.findViewById(R.id.tvSearchItemType);

            holder.txtItemLabel = (TextView)row.findViewById(R.id.tvItemLabel);
            holder.txtItemLabel.setTag(holder);
            holder.txtItemLabel.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    SearchItemHolder mH = (SearchItemHolder) v.getTag();
                    HandleSettingsClick(mH);
                }

                private void HandleSettingsClick(SearchItemHolder holderParm) {
                    if (holderParm.type.equals("1")){
                        // genre
                        SetGenre(holderParm);
                    } else if (holderParm.type.equals("2")){
                        // artist
                        SetArtist(holderParm);
                    } else if (holderParm.type.equals("3")){
                        // part
                        SetPart(holderParm);
                    }
                    data.set(position, searchItem);
                }


                private void SetPart(SearchItemHolder holderParm){
                    int partId = holderParm.id;
                    String partName = holderParm.txtItemLabel.getText().toString();
                    if (searchItem.Setting == null){
                        holder.txtItemLabel.setTextColor(Color.GREEN);
                        Constants.WriteToFile("include part " + partName);
                        Business.TellHouseIncludePartFromSettings(partId);
                        searchItem.Setting = true;
                    } else if (searchItem.Setting){
                        holder.txtItemLabel.setTextColor(Color.RED);
                        Constants.WriteToFile("telling house to exclude part " + partName);
                        Business.TellHouseExcludePartFromSettings(partId);
                        searchItem.Setting = false;
                    } else if (!searchItem.Setting) {
                        holder.txtItemLabel.setTextColor(Color.GRAY);
                        Constants.WriteToFile("clear part " + partName);
                        Business.TellHouseClearPartFromSettings(partId);
                        searchItem.Setting = null;
                    } else {
                        // shouldn't come here
                        holder.txtItemLabel.setTextColor(Color.CYAN);
                        Constants.WriteToFile("????? whu ????");
                    }
                }

                private void SetArtist(SearchItemHolder holderParm){
                    int artistId = holderParm.id;
                    String artistName = holderParm.txtItemLabel.getText().toString();
                    if (searchItem.Setting == null){
                        holder.txtItemLabel.setTextColor(Color.GREEN);
                        Constants.WriteToFile("include artist " + artistName);
                        Business.TellHouseIncludeArtistFromSettings(artistId);
                        searchItem.Setting = true;
                    } else if (searchItem.Setting){
                        holder.txtItemLabel.setTextColor(Color.RED);
                        Constants.WriteToFile("telling house to exclude artist " + artistName);
                        Business.TellHouseExcludeArtistFromSettings(artistId);
                        searchItem.Setting = false;
                    } else if (!searchItem.Setting) {
                        holder.txtItemLabel.setTextColor(Color.GRAY);
                        Constants.WriteToFile("clear artist " + artistName);
                        Business.TellHouseClearArtistFromSettings(artistId);
                        searchItem.Setting = null;
                    } else {
                        // shouldn't come here
                        holder.txtItemLabel.setTextColor(Color.CYAN);
                        Constants.WriteToFile("????? whu ????");
                    }
                }

                private void SetGenre(SearchItemHolder holderParm){
                    int genreId = holderParm.id;
                    String genreLabel = holderParm.txtItemLabel.getText().toString();
                    if (searchItem.Setting == null){
                        holder.txtItemLabel.setTextColor(Color.GREEN);
                        Constants.WriteToFile("include genre " + genreLabel);
                        Business.TellHouseIncludeGenreFromSettings(genreId);
                        searchItem.Setting = true;
                    } else if (searchItem.Setting){
                        holder.txtItemLabel.setTextColor(Color.RED);
                        Constants.WriteToFile("telling house to exclude genre " + genreLabel);
                        Business.TellHouseExcludeGenreFromSettings(genreId);
                        searchItem.Setting = false;
                    } else if (!searchItem.Setting) {
                        holder.txtItemLabel.setTextColor(Color.GRAY);
                        Constants.WriteToFile("clear genre " + genreLabel);
                        Business.TellHouseClearGenreFromSettings(genreId);
                        searchItem.Setting = null;
                    } else {
                        // shouldn't come here
                        holder.txtItemLabel.setTextColor(Color.CYAN);
                        Constants.WriteToFile("????? whu ????");
                    }
                }
            });
        }
        else
        {
            holder = (SearchItemHolder)row.getTag();
        }

        holder.id = searchItem.ItemId;
        holder.type = searchItem.ItemType;

        if (searchItem.Setting == null){
            // is not in the settings
            holder.txtItemLabel.setTextColor(Color.GRAY);
        } else if (searchItem.Setting){
            // is included in the settings
            holder.txtItemLabel.setTextColor(Color.GREEN);
        } else {
            // is excluded from the settings
            holder.txtItemLabel.setTextColor(Color.RED);
        }

        SettingBySearchItemType(holder, searchItem);

        return row;
    }

    private void SettingBySearchItemType(SearchItemHolder holder, SearchItem searchItem){
        String text = searchItem.ItemLabel;
        if (searchItem.ItemType.equals("1")){
            // genre
            holder.txtItemTypeLabel.setText("genre");
            holder.txtItemTypeLabel.setBackgroundColor(Constants.GenreBackGroundColor);
            holder.txtItemLabel.setBackgroundColor(Constants.GenreBackGroundColor);
        } else if (searchItem.ItemType.equals("2")){
            // artist
            holder.txtItemTypeLabel.setText("artist");
            holder.txtItemTypeLabel.setBackgroundColor(Constants.ArtistBackGroundColor);
            holder.txtItemLabel.setBackgroundColor(Constants.ArtistBackGroundColor);
        } else if (searchItem.ItemType.equals("3")){
            // part
            holder.txtItemTypeLabel.setText("part");
            holder.txtItemTypeLabel.setBackgroundColor(Constants.PartBackGroundColor);
            holder.txtItemLabel.setBackgroundColor(Constants.PartBackGroundColor);
        }
        holder.txtItemLabel.setText(text);
    }

    static class SearchItemHolder{
        TextView txtItemTypeLabel;
        TextView txtItemLabel;
        int id;
        String type;
    }
}
