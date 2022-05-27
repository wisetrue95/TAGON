package com.hyejin.TAGON;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.apache.commons.lang3.StringUtils;

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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;

public class SearchActivity extends Activity {
    public static Context mContext;

    //Naver api
    String clientId = "JOrbQRT1OnMPXqD8VeZZ";
    String clientSecret = "fYxraIG9SY";

    String poombun;

    //xml 뷰
    TextView searchResult;
    TextView poombun_main;
    TextView product_price_main;
    ImageView product_thumbnail_main;
    Button favoritebtn; // 즐겨찾기 버튼 추가

    //리스트뷰
    ListView listView;
    SearchActivity.ListViewAdapter listViewAdapter;
    //main 썸네일 데이터 flag
    boolean ascflag=true;

    //스피너뷰 -쇼핑몰 선택
    Spinner spinner;
    // 검색결과 리스트
    ArrayList<Results_ListItem> findingItems = new ArrayList<>();   //all
    ArrayList<Results_ListItem> gmarket = new ArrayList<>();        //gmarket
    ArrayList<Results_ListItem> st11 = new ArrayList<>();           //11st

    //데이터 베이스
    SQLiteDatabase favoitesDB ;

    //최종 배열
    ArrayList<ArrayList<String>> final_poombuns =new ArrayList<ArrayList<String>>();

    //후처리 함수 - 검색 결과 갯수 비교용 저장 리스트
    HashMap<String, Integer> countlist2 = new HashMap<>();  // 방법2


    // main
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_list);

        mContext=this;


        // 인터넷 연결
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectDiskReads().detectDiskWrites().detectNetwork().penaltyLog().build());

        //xml
        searchResult=findViewById(R.id.searchResult);
        poombun_main=findViewById(R.id.poombun);
        product_price_main=findViewById(R.id.product_price_main);
        product_thumbnail_main=findViewById(R.id.product_thumbnail_main);
        listView=findViewById(R.id.listView);
        spinner=findViewById(R.id.spinner);
        favoritebtn = findViewById(R.id.favoritebtn); // 즐겨찾기 버튼 추가

        //DB
        favoitesDB = init_database() ;
        init_tables() ;


        // 품번 변수 받아오기
        Intent intent = getIntent();
        poombun = intent.getExtras().getString("poombun");

        //대문자 변환
        poombun=poombun.toUpperCase();
        poombun=poombun.replace(" ","");
        //Toast.makeText(SearchActivity.this, "검색결과가 없습니다.", Toast.LENGTH_SHORT).show();


        // 비슷한 글자 있을 결구 검색 결과가 가장 많은 품번 찾기
        poombun=rePermutation(poombun);
        // 검색 시작
        NaverShopping(poombun);




        // 스피너 값 지정
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                //전체
                if(position==0){
                    //NaverShopping("all");
                    if(findingItems.size()!=0){
                        listViewAdapter = new ListViewAdapter(getApplicationContext(), findingItems);
                        listView.setAdapter(listViewAdapter);

                    }else{
                        spinner.setVisibility(View.INVISIBLE);
                        Intent intent = new Intent(getApplicationContext(), NoSearch.class);
                        startActivityForResult(intent, 1);
                        finish();
                        //Toast.makeText(SearchActivity.this, "검색결과가 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                }

                //G마켓
                if(position==1){
                    //NaverShopping("gmarket");
                    if(gmarket.size()!=0){
                        for(int i=0;i<gmarket.size();i++) {
                            listViewAdapter = new ListViewAdapter(getApplicationContext(), gmarket);
                            listView.setAdapter(listViewAdapter);
                        }
                    }else{
                        //Toast.makeText(SearchActivity.this, "검색결과가 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                }

                //11번가
                if(position==2){
                    //NaverShopping("11st");
                    if (st11.size() != 0) {
                        for (int i = 0; i < st11.size(); i++) {
                            listViewAdapter = new ListViewAdapter(getApplicationContext(), st11);
                            listView.setAdapter(listViewAdapter);
                        }
                    } else {
                        //Toast.makeText(SearchActivity.this, "검색결과가 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // 즐겨찾기 버튼 클릭시
        favoritebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //SearchActivity.this,
                Results_ListItem poombun_main = findingItems.get(0);
                String price = poombun_main.getPriceText();
                Bitmap thumbnail_main_value = poombun_main.getThumb();
                String img = BitMapToString(thumbnail_main_value);


                //Toast.makeText(getApplicationContext(), "즐겨찾기에 등록했습니다.", Toast.LENGTH_LONG).show();


                //품번 string 대소문자 구분
                //데이터 베이스 저장
                save_values(poombun, price, img);
                searchResult.setText(poombun);
            }
        });

    }


    //-- 함수

    // 최저가 검색
    public void NaverShopping(String poombun){
        try{


            String text = URLEncoder.encode(poombun, "UTF-8"); //poombun

            // 네이버 api 검색 조건: display: 항목개수, sort=asc:최저가순
            String apiURL = "https://openapi.naver.com/v1/search/shop.xml?query="+ text
                    +"&display=50" +"&sort=asc"; // xml 결과

            URL url = new URL(apiURL);
            HttpURLConnection con = (HttpURLConnection)url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("X-Naver-Client-Id", clientId);
            con.setRequestProperty("X-Naver-Client-Secret", clientSecret);
            int responseCode = con.getResponseCode();
            BufferedReader br;


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

            String total_value=Total(shopResult);
            //Toast.makeText(getApplicationContext(),total_value,Toast.LENGTH_LONG).show();




            // 모든 항목 for 루프
            for(Shop shop : parsingResult) {
                Bitmap thumbImg = getBitmapFromURL(shop.getImage());
                // 출력 형식
                findingItems.add(new Results_ListItem(shop.getTitle(),thumbImg,
                        shop.getLprice() + "원", shop.getLprice(),
                        shop.getLink(),
                        shop.getMallName()));
            }
            if(findingItems.size()!=0) {
                //품번 정보 맨 상위에 표시
                // SearchActivity 호출 일때 (fav==0)
                poombun_main.setText("NO. "+poombun);

                // 리스트뷰 각 항목 출력
                for (int i = 0; i < findingItems.size(); i++) {


                    // main 정보 가져오기
                    if (i == 0 && ascflag == true) {

                        Results_ListItem poombun_main_list = findingItems.get(0);
                        String price_main_value = poombun_main_list.getPrice()+"원";
                        Bitmap thumbnail_main_value = poombun_main_list.getThumb();
                        product_price_main.setText("최저가: " + price_main_value);
                        product_thumbnail_main.setImageBitmap(thumbnail_main_value);



                    }
                    ascflag = false;


                    // mall 정보 각 리스트에 저장하기
                    // 1) G마켓
                    Results_ListItem gmarket_list = findingItems.get(i);

                    if (gmarket_list.getMallName().equals("G마켓")) {
                        gmarket.add(findingItems.get(i));
                    }

                    // 2) 11번가
                    Results_ListItem st11_list = findingItems.get(i);
                    if (st11_list.getMallName().equals("11번가")) {
                        st11.add(findingItems.get(i));
                    }
                }
            }

            //예외처리
        } catch (Exception e) {
            System.out.println(e);
            //searchResult.setText("에러남:  "+e.toString());   //에러표시

        }



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


    // 검색 결과 개수 구하기 함수
    public String Total(String data) throws Exception {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        XmlPullParser parser = factory.newPullParser();

        parser.setInput(new StringReader(data));
        int eventType = parser.getEventType();
        String total="";


        while (eventType != XmlPullParser.END_DOCUMENT) {
            switch (eventType) {
                case XmlPullParser.END_DOCUMENT:
                    break;
                case XmlPullParser.START_TAG: {
                    // item태그속 데이터 분류
                    String tag = parser.getName();
                    if (tag.equals("total")) {
                        eventType = parser.next();
                        total=parser.getText();
                        System.out.println("total="+total);
                        break;

                    }
                }
            }eventType = parser.next();
        }
        return total;
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

    //--------------------------------------------------------------------------------------------
    // --후처리 함수
    /* 후처리 함수 작동 방식
        1. [rePermutation()]    input : poombun / output : poombun
                                앞에서 넘어온 poombun의  경우의 수 고려 (영어"O" 와 숫자"0"가 있다면)
                                각각의 경우의 수 마다 search_count(poombun) 발생

        2. [search_count()]     input : poombun / output : int array
                                방법1. 품번을 넘겨받으면, 해당 품번의 검색 갯수를 int 리스트에 저장
                                [Descending()] 리스트 내림차순 정렬

                                -->> (변경)
                                방법2. 품번을 넘겨받으면, 해당 품번의 <검색 갯수(int),poombun(String)> 으로 hashmap 저장
                                hashmap > treemap 으로 key 값 내림차순 정렬

        3. [rePermutation()]    input : poombun / output : poombun
                                가장 큰 값 가져오기 (리스트[0] 로 가져오면 될듯)
                                -->> 해당 값의 poombun을 가져와야 하는데, how? --> hashmap

                                hashmap 에 key : 검색갯수 / value : poombun으로 저장, key 정렬,
                                https://jobc.tistory.com/176

     */

    public String rePermutation(String abc) {


        // 품번 abc

        //비슷한 글자 (0, O)
        String []n = {"0","O"};

        // 비슷한 글자 공통 * 처리
        abc = abc.replace("0","*");
        abc = abc.replace("O","*");
        String replaced_poombun = abc; //처리된 품번변수
        System.out.println("Test : 품번1"+abc);

        //자리수(*개수)
        int r = StringUtils.countMatches(replaced_poombun, "*");
        System.out.println("r개수: "+r);
        //int r = 3;

        //중복순열
        LinkedList<String> rCom = new LinkedList<String>();

        rePermutation1(n, r, rCom);

        System.out.println();
        System.out.println("Test : 파이널 품번"+final_poombuns);
        System.out.println();

        // 변형된 문자열이 있는 배열
        //ArrayList<String> changed =new ArrayList<String>();

        //*자리 중복 순열로 채우기
        for(ArrayList<String> s : final_poombuns){
            for(String k : s){
                abc = abc.replaceFirst("\\*",k);
                //System.out.println(poombun);
            }
            System.out.println("Test : 품번2"+abc);
            //changed.add(poombun);   // 이 품번을 배열에 넣지 말고 개수 구하는 함수 사용해도 될듯
            search_count(abc);

            // 다시 처리된 품번 변수로 초기화
            abc = replaced_poombun;

        }

        System.out.println("됨1"+countlist2);

        List<String> keySetList = new ArrayList<>(countlist2.keySet());
        // 내림차순 //
        Collections.sort(keySetList, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return countlist2.get(o2).compareTo(countlist2.get(o1));
            }
        });

        //제일 많은 검색 결과 개수를 가진 품번
        String bestpoombun="";

        //--내림 차수 정렬 프린트 테스트
        for(String key : keySetList) {
            System.out.println(String.format("Key : %s, Value : %s", key, countlist2.get(key)));
        }

        // 내림차순 정렬
        for(String key : keySetList) {
            System.out.println(String.format("Key : %s, Value : %s", key, countlist2.get(key)));
            bestpoombun=key; // 내림차순 첫번째(제일큰것)
            break;

        }

        System.out.println("가장 개수 많은 품번"+bestpoombun);

        return bestpoombun;


    }
    // 중복 순열 경우의 수 만들기
    private void rePermutation1(String[] n, int r, LinkedList<String> rCom) {


        if(rCom.size() == r){

            ArrayList<String> list2=new ArrayList(); //배열이 들어가는 리스트
            for(String i : rCom){
                System.out.print("변형품번 "+i+" ");
                // 만든 경우의수 배열
                list2.add(i);
            }

            System.out.println();
            System.out.println(list2);

            final_poombuns.add(list2); //최종 배열에 삽입

            return;
        }

        for(String c : n){

            rCom.add(c);
            rePermutation1(n, r, rCom);
            rCom.removeLast();
        }

    }

    // 검색 결과 int값 도출 + 리스트 저장
    //후처리 함수 - 검색 갯수만 나오게 search_count
    private void search_count(String abc){

        try{
            String text = URLEncoder.encode(abc, "UTF-8"); //poombun

            // 네이버 api 검색 조건: display: 항목개수, sort=asc:최저가순
            String apiURL = "https://openapi.naver.com/v1/search/shop.xml?query="+ text
                    +"&display=100" +"&sort=asc"; // xml 결과

            // 후처리 함수 추가

            URL url = new URL(apiURL);
            HttpURLConnection con = (HttpURLConnection)url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("X-Naver-Client-Id", clientId);
            con.setRequestProperty("X-Naver-Client-Secret", clientSecret);
            int responseCode = con.getResponseCode();
            BufferedReader br;


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

            // 검색 결과 구하기 함수(Total()) 불러와서 poombun 넣고 돌리기. => 결과값 int.
            int total_count = Integer.parseInt(Total(shopResult));
            System.out.println("토탈카운트 나오나: "+total_count);


            // -- 방법2. 자동 정렬 해주는 Treemap으로

            // hashmap list에 (key : 검색결과수 total_count, value : 해당 값의 poombun) 집어넣기
            //countlist2.put(total_count, abc);
            countlist2.put(abc, total_count);




            //예외처리
        } catch (Exception e) {
            System.out.println(e);
        }
    }


    //--------------------------------------------------------------------------------------------
    //--DB
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

        // 테이블 열 [POOMBUN | PRICE | IMG]
        if (favoitesDB != null) {
            String sqlCreateTbl = "CREATE TABLE IF NOT EXISTS CONTACT_T (" +
                    "POOMBUN "         + "TEXT," +
                    "PRICE "        + "TEXT," +
                    "IMG "       + "TEXT" + ")" ;

            System.out.println(sqlCreateTbl) ;
            favoitesDB.execSQL(sqlCreateTbl) ;
        }
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
        Toast.makeText(getApplicationContext(), "스크랩 완료!", Toast.LENGTH_LONG).show();


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

    //--------------------------------------------------------------------------------------------

   //--리스트뷰
    private class ListViewAdapter extends BaseAdapter {
        Context context;
        ArrayList<SearchActivity.Results_ListItem> listItems;

        private ListViewAdapter(Context context, ArrayList<SearchActivity.Results_ListItem> listItems) {
            this.context = context;
            this.listItems = listItems;
        }

        public int getCount() {
            return listItems.size();
        }

        public SearchActivity.Results_ListItem getItem(int position) {
            return listItems.get(position);
        }

        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            // 뷰 홀더 적용: 리스트뷰 중첩 제거
            final UserViewHolder  userViewHolder;

            if(convertView == null) {

                LayoutInflater inflater = LayoutInflater.from(context);
                convertView=inflater.inflate(R.layout.list_item, parent, false);

                //list_item에 정보 맵핑
                userViewHolder = new UserViewHolder();
                userViewHolder.thumbView = convertView.findViewById(R.id.product_thumbnail);
                userViewHolder.productNameView = convertView.findViewById(R.id.product_name);
                userViewHolder.priceView = convertView.findViewById(R.id.product_price);
                userViewHolder.mallNameView = convertView.findViewById(R.id.product_mallname);

                //링크 연결
                convertView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(userViewHolder.shoppingmall_url));
                        startActivity(myIntent);
                    }
                });
                convertView.setTag(userViewHolder);


            } else {
                userViewHolder = (UserViewHolder) convertView.getTag();


            }
            userViewHolder.bind(this.context, this.listItems.get(position));


            return convertView;
        }
    }

    // 리스트뷰 아이템 리스트 뷰에 들어갈 정보를 담고 있다
    class Results_ListItem{
        private Bitmap thumb;
        private String productName;
        private String price;
        private int int_price;
        private String url;
        private String mallName;

        public Results_ListItem(String productName, Bitmap thumb, String price, int int_price, String url,
                               String mallName) {
            this.thumb = thumb;
            this.productName = productName;
            this.price = price;
            this.int_price = int_price;
            this.url = url;
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

    // 리스트뷰 뷰, xml과 연결된 레이아웃 클래스
    // 뷰홀더: 리스트뷰 중첩 해결
    public static class UserViewHolder {
        ImageView thumbView;
        TextView productNameView;
        TextView priceView;
        TextView mallNameView;
        String shoppingmall_url;

        public void bind(Context context, final SearchActivity.Results_ListItem aItem) {

            thumbView.setImageBitmap(aItem.getThumb());
            productNameView.setText(aItem.getProductName());
            priceView.setText(aItem.getPriceText());
            mallNameView.setText(aItem.getMallName());
            shoppingmall_url=aItem.getUrl();


        }
    }

}

