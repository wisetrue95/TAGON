package com.hyejin.TAGON;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.preference.Preference;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;


import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Timer;
import java.util.TimerTask;

public class Favorites extends Activity {

    //Naver api
    String clientId = "JOrbQRT1OnMPXqD8VeZZ";
    String clientSecret = "fYxraIG9SY";

    String poombun;

    //xml 뷰

    ImageView grid_imageview;
    TextView grid_textview_name;
    TextView grid_textview_price;
    ToggleButton toggle;
    Button grid_deletebutton;

    //그리드뷰
    GridView gridView_favorites;
    //SingerAdapter mMyAdapter;

    // DB
    SQLiteDatabase favoitesDB ;

    // 가격 비교
    int new_price;

    Timer timer;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.singer_main);

        // 메인 쓰레드에서 네트워크 사용 에러해결: android.os.NetworkOnMainThreadException
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        gridView_favorites=findViewById(R.id.gridView);

        // DB
        favoitesDB = init_database() ;
        init_tables();  // 테이블 생성
        load_values();  // 데이터 로드


    }

    // 푸시메세지 함수
    private void createNotification() {

        PendingIntent mPendingIntent = PendingIntent.getActivity(Favorites.this, 0,
                new Intent(getApplicationContext(), Favorites.class),
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "default");

        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentTitle("TAGON");
        builder.setContentText(poombun+" 더 저렴한 가격 발견!");

        builder.setWhen(System.currentTimeMillis());

        // 사용자가 탭을 클릭하면 자동 제거
        builder.setAutoCancel(true);
        builder.setDefaults(Notification.DEFAULT_VIBRATE); //진동으로 알림

        // 알림 표시
        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(new NotificationChannel("default", "기본 채널", NotificationManager.IMPORTANCE_DEFAULT));
        }

        // id값은
        // 정의해야하는 각 알림의 고유한 int값
        notificationManager.notify(1, builder.build());
    }


    //--리스트뷰 -> 그리드뷰 어댑터
    class SingerAdapter extends BaseAdapter {
        Context context;
        ArrayList<SingerItem> items = new ArrayList<SingerItem>();
        @Override
        public int getCount() {
            return items.size();
        }

        public void addItem(SingerItem singerItem){
            items.add(singerItem);
        }

        @Override
        public SingerItem getItem(int i) {
            return items.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View convertView, ViewGroup parent) {

            final int pos = i ; // 아이템 위치
            final Context context = parent.getContext();

            // 'listview_custom' Layout을 inflate하여 convertView 참조 획득
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.singeritem, parent, false);
            }

            //singeritem에 정보 맵핑
            grid_textview_name = convertView.findViewById(R.id.singertextview1);
            grid_textview_price = convertView.findViewById(R.id.singertextview2);
            grid_imageview = convertView.findViewById(R.id.singerimageview1);
            grid_deletebutton=convertView.findViewById(R.id.singerbutton2);
            toggle=convertView.findViewById(R.id.toggleButton);


            /* 각 리스트에 뿌려줄 아이템을 받아오는데 mMyItem 재활용 */
            SingerItem myItem = getItem(i);

            /* 각 위젯에 세팅된 아이템을 뿌려준다 */
            grid_textview_name.setText(myItem.getName());
            grid_textview_price.setText(myItem.getprice());
            grid_imageview.setImageBitmap(myItem.getImage());


            // 그리드뷰 아이템 클릭할때
            gridView_favorites.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                // 리스트 아이템 클릭
                public void onItemClick(AdapterView parent, View v, int position, long id) {
                    // 아이템 위치(pos+1) 출력
                    //Toast.makeText(getApplicationContext(), Integer.toString(position+1) + " Item is selected..", Toast.LENGTH_SHORT).show() ;

                    // 현재 품번 가져오기
                    String fav_poombun= items.get(position).getName();
                    //Toast.makeText(getApplicationContext(), fav_poombun, Toast.LENGTH_SHORT).show() ;

                    // SearchActivity 전환후 품번 검색
                    Intent intent = new Intent(getApplicationContext(), SearchActivity.class);
                    intent.putExtra("poombun", fav_poombun);
                    startActivity(intent);
                    finish();
                }

            }) ;


            // 삭제 button 클릭 시 데이터베이스 삭제
            grid_deletebutton.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v) {

                    // 현재 품번 가져오기
                    String delete_poombun= items.get(pos).getName();  // 해당 아이템 위치(pos)에 있는 텍스트 가져오기
                    //Toast.makeText(getApplicationContext(), delete_poombun, Toast.LENGTH_LONG).show();
                    Toast.makeText(getApplicationContext(), "삭제되었습니다.", Toast.LENGTH_LONG).show();

                    // 해당 품번 데이터 삭제
                    favoitesDB.execSQL(" DELETE FROM CONTACT_T WHERE POOMBUN = '" + delete_poombun + "'; ");

                    // 그리드뷰 최신화
                    load_values();
                }
            });


            // 푸시메세지 알림 버튼 (Toggle)
            toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    // toggle ON
                    if (isChecked) {
                        // 시간 반복
                        TimerTask tt=new TimerTask() {
                            @Override
                            public void run() {
                                // 쓰레드
                                Handler mHandler = new Handler(Looper.getMainLooper());
                                mHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {

                                        // 현재 품번 가져오기
                                        String previous_poombun= items.get(pos).getName();  // 해당 아이템 위치(pos)에 있는 텍스트 가져오기
                                        //Toast.makeText(getApplicationContext(), previous_poombun, Toast.LENGTH_LONG).show();

                                        // 검색결과 리스트 push
                                        ArrayList<Push_ListItem> findingItems = new ArrayList<>();

                                        // Navershopping 함수
                                        poombun=previous_poombun;
                                        try{

                                            String text = URLEncoder.encode(poombun, "UTF-8"); //poombun

                                            // 네이버 api 검색 조건: display: 항목개수, sort=asc:최저가순
                                            //fav는 검색개수 5로 함 : display=5
                                            String apiURL = "https://openapi.naver.com/v1/search/shop.xml?query="+ text
                                                    +"&display=5" +"&sort=asc"; // xml 결과

                                            URL url = new URL(apiURL);
                                            HttpURLConnection con = (HttpURLConnection)url.openConnection();
                                            con.setRequestMethod("GET");
                                            con.setRequestProperty("X-Naver-Client-Id", clientId);
                                            con.setRequestProperty("X-Naver-Client-Secret", clientSecret);
                                            int responseCode = con.getResponseCode();
                                            BufferedReader br;

                                            //Toast.makeText(getApplicationContext(), "이상없음", Toast.LENGTH_LONG).show();

                                            if(responseCode==200) { // 정상 호출
                                                br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                                            } else {  // 에러 발생
                                                br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
                                            }
                                            String inputLine;
                                            StringBuffer response = new StringBuffer();
                                            while ((inputLine = br.readLine()) != null) {
                                                response.append(inputLine);
                                            }
                                            br.close();
                                            System.out.println(response.toString());

                                            // 네이버 api 검색 결과 가져와 리스트 만들기
                                            String shopResult=response.toString();
                                            List<Shop> parsingResult = parsingShopResultXml(shopResult);

                                            // 모든 항목 for 루프
                                            for(Shop shop : parsingResult) {
                                                Bitmap thumbImg = getBitmapFromURL(shop.getImage());
                                                // 출력 형식
                                                findingItems.add(new Push_ListItem(shop.getTitle(),thumbImg,
                                                        shop.getLprice() + "원", shop.getLprice(),
                                                        shop.getLink(),
                                                        shop.getMallName()));
                                            }
                                            if(findingItems.size()!=0) {
                                                //첫번째 품번 정보(최저가)
                                                Push_ListItem poombun_main_list = findingItems.get(0);
                                                new_price = poombun_main_list.getPrice();
                                                //Toast.makeText(getApplicationContext(), poombun+" "+Integer.toString(new_price), Toast.LENGTH_LONG).show();
                                            }
                                        } catch (Exception e) { //예외처리
                                            System.out.println(e);
                                            //Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
                                        }

                                        //--가격비교

                                        // 이전 가격
                                        CharSequence previous_price = items.get(pos).getprice();
                                        String previous_price_string = previous_price.toString().substring(0, previous_price.length() - 1);
                                        int previous_price_int = Integer.parseInt(previous_price_string);

                                        //가격 전보다 작으면 <
                                        if (previous_price_int <= new_price) {    //test 등호 바꿔야함

                                            Toast.makeText(getApplicationContext(), "품번: " + previous_poombun + " 이전가격: " + previous_price_string + " 새로운가격: " + Integer.toString(new_price), Toast.LENGTH_LONG).show();
                                            createNotification(); //푸시알림 함수

                                            //DB수정
                                            Push_ListItem poombun_main = findingItems.get(0);
                                            String price = Integer.toString(new_price) + "원";
                                            Bitmap thumbnail_main_value = poombun_main.getThumb();
                                            String img = BitMapToString(thumbnail_main_value);

                                            //데이터 베이스 저장> 즐겨찾기 갱신
                                            save_values(previous_poombun, price, img);
                                            //Toast.makeText(getApplicationContext(), "갱신!", Toast.LENGTH_LONG).show();

                                            previous_price_int=new_price;

                                        }else if(previous_price_int > new_price){ // 가격이 전보다 클때
                                            Toast.makeText(getApplicationContext(), "품번: " + previous_poombun + " 이전가격: " + previous_price_string + " 새로운가격: " + Integer.toString(new_price), Toast.LENGTH_LONG).show();

                                            //DB수정
                                            Push_ListItem poombun_main = findingItems.get(0);
                                            String price = Integer.toString(new_price) + "원";
                                            Bitmap thumbnail_main_value = poombun_main.getThumb();
                                            String img = BitMapToString(thumbnail_main_value);

                                            //데이터 베이스 저장> 즐겨찾기 갱신
                                            save_values(previous_poombun, price, img);

                                        }
                                        else {

                                            Toast.makeText(getApplicationContext(), "가격 변동 없음 "+"품번: " + previous_poombun + " 이전가격: " + previous_price_string + " 새로운가격: " + Integer.toString(new_price), Toast.LENGTH_LONG).show();
                                        }
                                    }
                                }, 0);
                            }
                        };

                        timer = new Timer();
                        timer.schedule(tt,0,10000); //1분에 1번

                    }else{
                        // timer 삭제
                        timer.cancel();
                        timer.purge();
                        timer=null;

                        // 그리드뷰 최신화
                        //load_values();
                    }
                }
            });

            return convertView;
        }

        /* 아이템 데이터 추가를 위한 함수. 자신이 원하는대로 작성 */
        public void addItem(String name, String price, Bitmap img) {

            SingerItem mItem = new SingerItem(name, price,img);

            /* MyItem에 아이템을 setting한다. */
            mItem.setImage(img);
            mItem.setName(name);
            mItem.setprice(price);

            /* mItems에 MyItem을 추가한다. */
            items.add(mItem);

        }

    }




    // DB
    // DB 생성/열기
    private SQLiteDatabase init_database() {

        SQLiteDatabase db = null ;
        // File file = getDatabasePath("contact.db") ;
        File file = new File(getFilesDir(), "contact.db") ;

        System.out.println("PATH : " + file.toString()) ;
        try {
            db = SQLiteDatabase.openOrCreateDatabase(file, null) ;
        } catch (SQLiteException e) {
            e.printStackTrace() ;
        }

        if (db == null) {
            System.out.println("DB creation failed. " + file.getAbsolutePath()) ;
        }

        return db ;
    }
    // DB 테이블 만들기
    private void init_tables() {

        if (favoitesDB != null) {
            String sqlCreateTbl = "CREATE TABLE IF NOT EXISTS CONTACT_T (" +
                    "POOMBUN "         + "TEXT," +
                    "PRICE "        + "TEXT," +
                    "IMG "       + "TEXT" + ")" ;

            System.out.println(sqlCreateTbl) ;

            favoitesDB.execSQL(sqlCreateTbl) ;
        }
    }
    // DB 데이터 즐겨찾기에 로드
    private void load_values() {
        SingerAdapter mMyAdapter = new SingerAdapter();

        if (favoitesDB != null) {
            String sqlQueryTbl = "SELECT * FROM CONTACT_T" ;
            Cursor cursor = null ;

            // 쿼리 실행
            cursor = favoitesDB.rawQuery(sqlQueryTbl, null) ;

            if (cursor.moveToNext()) { // 레코드가 존재한다면,

                // 즐겨찾기 항목 개수
                int cnt=cursor.getCount();
                //Toast.makeText(getApplicationContext(), Integer.toString(cnt), Toast.LENGTH_LONG).show();



                do {
                    // poombunDB (TEXT) 값 가져오기.
                    String poombunDB = cursor.getString(0);
                    // price (TEXT) 값 가져오기
                    String price = cursor.getString(1);
                    // img (TEXT) 값 가져오기
                    String img = cursor.getString(2);
                    // 그리드 뷰 항목에 추가
                    mMyAdapter.addItem(new SingerItem(poombunDB,
                            price, StringToBitmap(img)));

                }while (cursor.moveToNext()); // 모든 항목 그리드 뷰에 만들기


            }
        }
        // 리스트뷰에 어댑터 등록
        gridView_favorites.setAdapter(mMyAdapter);
        // 그리드뷰 최신화(삭제할때)
        mMyAdapter.notifyDataSetChanged();
    }
    // DB 데이터 추가
    private void save_values(String poombunDB, String price, String img) {
        //대문자 변환
        poombunDB=poombunDB.toUpperCase();

        //각 값 테이블에 넣기
        String sqlInsert = "INSERT INTO CONTACT_T " +
                "(POOMBUN, PRICE, IMG) VALUES (" +
                "'" + poombunDB + "'," +
                "'" + price + "'," +
                "'" + img + "'"+ ")" ;


        //Toast.makeText(getApplicationContext(), poombunDB+" "+price, Toast.LENGTH_LONG).show();


        //같은 데이터 삭제
        favoitesDB.execSQL(" DELETE FROM CONTACT_T WHERE POOMBUN = '" + poombunDB + "'; ");


        System.out.println(sqlInsert) ;
        favoitesDB.execSQL(sqlInsert) ;

    }
    //이미지를 스트링으로
    public String BitMapToString(Bitmap bitmap){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG,100,baos);
        byte [] b = baos.toByteArray();
        String temp = Base64.encodeToString(b,Base64.DEFAULT);
        return temp;
    }


    // xml 형태의 결과를 파싱하는 작업
    public List<Shop> parsingShopResultXml(String data) throws Exception {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        XmlPullParser parser = factory.newPullParser();

        List<Shop> list = null ;
        parser.setInput(new StringReader(data));
        int eventType = parser.getEventType();
        Shop b = null;

        while (eventType != XmlPullParser.END_DOCUMENT) {
            switch (eventType) {
                case XmlPullParser.START_DOCUMENT:
                    list = new ArrayList<Shop>();
                    break;
                case XmlPullParser.END_DOCUMENT:
                    break;
                case XmlPullParser.START_TAG: {
                    // item태그속 데이터 분류
                    String tag = parser.getName();
                    switch (tag) {
                        case "item":
                            b = new Shop();
                            break;
                        case "title":
                            if (b != null) {
                                b.setTitle(RemoveHTMLTag(parser.nextText()));
                            }
                            break;
                        case "link":
                            if (b != null)
                                b.setLink(parser.nextText());
                            break;
                        case "image":
                            if (b != null)
                                b.setImage(parser.nextText()+"?type=f140");
                            break;
                        case "total":
                            if (b != null)
                                b.setTotal(parser.next());
                            break;
                        case "lprice":
                            if (b != null)
                                b.setLprice(Integer.parseInt(parser.nextText()));
                        case "hprice":
                            if (b != null)
                                b.setHprice(parser.next());
                            break;
                        case "mallName":
                            if (b != null)
                                b.setMallName(RemoveHTMLTag(parser.nextText()));
                            break;
                    }
                    break;
                }
                case XmlPullParser.END_TAG: {
                    String tag = parser.getName();
                    if (tag.equals("item")) {
                        list.add(b);
                        b = null;
                    }
                }
            }
            eventType = parser.next();
        }
        return list;
    }
    // 결과 태그 제거
    public String RemoveHTMLTag(String changeStr) {
        if (changeStr != null && !changeStr.equals("")) {
            changeStr = changeStr.replaceAll("<(/)?([a-zA-Z]*)(\\s[a-zA-Z]*=[^>]*)?(\\s)*(/)?>", "");
        } else {
            changeStr = "";
        }
        return changeStr;
    }
    // 이미지
    public Bitmap getBitmapFromURL(String src) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(src);
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            BitmapFactory.Options op = new BitmapFactory.Options();
            op.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap myBitmap = BitmapFactory.decodeStream(input, null, op);
            return myBitmap;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
    // 리스트뷰 아이템 리스트 뷰에 들어갈 정보를 담고 있다
    class Push_ListItem{
        private Bitmap thumb;
        private String productName;
        private String price;
        private int int_price;
        private String url;
        private ArrayList<String> keywords;
        private String combinationKeyword;
        private String thumbUrl;
        private String mallName;

        public Push_ListItem(String productName, Bitmap thumb, String price, int int_price, String url,
                             String mallName) {
            this.thumb = thumb;
            this.productName = productName;
            this.price = price;
            this.int_price = int_price;
            this.url = url;
            this.keywords = keywords;
            this.combinationKeyword = combinationKeyword;
            this.thumbUrl = thumbUrl;
            this.mallName = mallName;

        }

        public int getPrice() { return this.int_price; }
        public String getPriceText() { return this.price; }
        public Bitmap getThumb() { return this.thumb; }
        public String getProductName() { return this.productName; }
        public String getUrl() { return this.url; }
        public String getMallName() {
            return mallName;
        }
    }



    // String형을 BitMap으로 변환시켜주는 함수
    public Bitmap StringToBitmap(String encodedString) {
        try { byte[] encodeByte = Base64.decode(encodedString, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
            return bitmap;
        } catch (Exception e) {
            e.getMessage(); return null;
        }
    }



}