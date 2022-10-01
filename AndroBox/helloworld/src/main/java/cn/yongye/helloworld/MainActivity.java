package cn.yongye.helloworld;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ListView listView = null;
    static Context mContext = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(HelloApp.TAG, "[MainActivity] onCreate called.");
        new Throwable().printStackTrace();
        mContext = getApplicationContext();
        listView = findViewById(R.id.list_item);
        ArrayList<String> mData = new ArrayList<>();
        mData.add("启动SencondActivity");
        mData.add("发送广播消息");
        ItemAdapter itemAdapter = new ItemAdapter(MainActivity.this, mData);
        listView.setAdapter(itemAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent();
                switch (i) {
                    case 0:
                        Toast.makeText(MainActivity.this, "启动SencondActivity", Toast.LENGTH_SHORT).show();
                        MainActivity.this.startActivity(new Intent(MainActivity.this, SencondActivity.class));
                        break;
                    case 1:
                        intent.setAction("cn.yongye.helloworld.selfrecevier");
                        intent.putExtra("msg", "这是一个广播消息。");
                        mContext.sendBroadcast(intent);
                        break;
                }
            }
        });
    }

    public class ItemAdapter extends BaseAdapter {

        ArrayList<String> mData  = null;
        Context mContext = null;

        public ItemAdapter(Context context, ArrayList<String> mData) {
            this.mContext = context;
            this.mData = mData;
        }

        @Override
        public int getCount() {
            return mData.size();
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            view = LayoutInflater.from(mContext).inflate(R.layout.view_list_item, viewGroup, false);
            TextView nameView = view.findViewById(R.id.itemTitle);
            nameView.setText(mData.get(i));
            return view;
        }
    }
}