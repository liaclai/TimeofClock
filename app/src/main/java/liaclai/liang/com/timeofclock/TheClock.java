package liaclai.liang.com.timeofclock;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class TheClock extends AppCompatActivity implements View.OnClickListener{

    private String TAG = "TheClock";

    private Helix mHelix;

    public Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    mHelix.invalidate();
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_the_clock);
        mHelix = (Helix) findViewById(R.id.helix);
        Thread thread = new Thread(new Refresh());
        thread.start();
    }

    @Override
    protected void onResume(){
        super.onResume();
        mHelix.invalidate();
    }

    /**
     * 刷新
     */
    private void refresh() {
        setContentView(R.layout.activity_the_clock);
        mHelix = (Helix) findViewById(R.id.helix);
    }

    @Override
    public void onClick(View v) {

    }

    private class Refresh implements Runnable{
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()){
                try {
                    Thread.currentThread().sleep(2000);
                    Message msg = new Message();
                    msg.what = 0;
                    handler.sendMessage(msg);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
