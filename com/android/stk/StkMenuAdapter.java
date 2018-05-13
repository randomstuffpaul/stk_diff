package com.android.stk;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.internal.telephony.cat.CatLog;
import com.android.internal.telephony.cat.Item;
import java.util.List;

public class StkMenuAdapter extends ArrayAdapter<Item> {
    private boolean mIcosSelfExplanatory = false;
    private final LayoutInflater mInflater;

    public StkMenuAdapter(Context context, List<Item> items, boolean icosSelfExplanatory) {
        super(context, 0, items);
        this.mInflater = LayoutInflater.from(context);
        this.mIcosSelfExplanatory = icosSelfExplanatory;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        Item item = (Item) getItem(position);
        if (convertView == null) {
            convertView = this.mInflater.inflate(R.layout.stk_menu_item, parent, false);
        }
        try {
            if (!this.mIcosSelfExplanatory || (this.mIcosSelfExplanatory && item.icon == null)) {
                ((TextView) convertView.findViewById(R.id.text)).setText(item.text);
            }
            ImageView imageView = (ImageView) convertView.findViewById(R.id.icon);
            if (item.icon == null) {
                imageView.setVisibility(8);
            } else {
                imageView.setImageBitmap(item.icon);
                imageView.setVisibility(0);
            }
        } catch (IndexOutOfBoundsException e) {
            CatLog.d(this, "Invalid menu");
        } catch (NullPointerException e2) {
            CatLog.d(this, "Invalid menu");
        }
        return convertView;
    }
}
