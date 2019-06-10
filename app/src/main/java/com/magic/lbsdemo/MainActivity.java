package com.magic.lbsdemo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.blankj.utilcode.util.DeviceUtils;
import com.blankj.utilcode.util.Utils;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private LocationClient mLocationClient;
    private MapView mMapView;
    private TextView positionText;
    private BaiduMap mBaiduMap;
    private double mLatitude;
    private boolean isFirstLocate = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.init(this);
        mLocationClient = new LocationClient(getApplicationContext());
        mLocationClient.registerLocationListener((BDAbstractLocationListener) new MyLocationListener());
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);
        positionText = findViewById(R.id.postion_text_view);
        mMapView = findViewById(R.id.mapView);
        mBaiduMap = mMapView.getMap();
        mBaiduMap.setMyLocationEnabled(true);


        List<String> permissionList = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.
                permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!permissionList.isEmpty()) {
            String[] permissions = permissionList.toArray(new String[permissionList.
                    size()]);
            ActivityCompat.requestPermissions(MainActivity.this, permissions, 1);
        } else {
            requestLocation();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0) {
                    for (int result : grantResults) {
                        if (result != PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(this, "必须同意所有权限才能使用本程序", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                    }
                    requestLocation();
                } else {
                    Toast.makeText(this, "发生未知错误", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocationClient.stop();
        mMapView.onDestroy();
        mBaiduMap.setMyLocationEnabled(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    private void requestLocation() {
        initLocation();
        mLocationClient.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    //实时更新位置数据
    private void initLocation() {
        LocationClientOption option = new LocationClientOption();
        option.setScanSpan(1000);
        //强制指定只使用 GPS 进行定位
        //option.setLocationMode(LocationClientOption.LocationMode.Device_Sensors);
        //表示我们需要获取当前位置详细的地址信息
        option.setIsNeedAddress(true);
        //打开GPS 会实时显示经纬度(不停变化)
        option.setOpenGps(true);
        //如果没有下一句  会导致定位不准
        option.setCoorType("bd09ll");
        //获取海拔
        option.setIsNeedAltitude(true);
        mLocationClient.setLocOption(option);
    }


    private class MyLocationListener extends BDAbstractLocationListener implements BDLocationListener {

        private double mLongitude;

        @Override
        public void onReceiveLocation(final BDLocation bdLocation) {
            if (bdLocation.getLocType() == BDLocation.TypeGpsLocation || bdLocation.getLocType() == BDLocation.TypeNetWorkLocation) {
                navigateTo(bdLocation);
            }
            //BDLocation 回调的百度坐标类，内部封装了如经纬度、半径等属性信息
            //MyLocationData 定位数据,定位数据建造器
            /*
             * 可以通过BDLocation配置如下参数
             * 1.accuracy 定位精度
             * 2.latitude 百度纬度坐标
             * 3.longitude 百度经度坐标
             * 4.satellitesNum GPS定位时卫星数目 getSatelliteNumber() gps定位结果时，获取gps锁定用的卫星数
             * 5.speed GPS定位时速度 getSpeed()获取速度，仅gps定位结果时有速度信息，单位公里/小时，默认值0.0f
             * 6.direction GPS定位时方向角度
             * */

            mLatitude = bdLocation.getLatitude();
            mLongitude = bdLocation.getLongitude();
            MyLocationData data = new MyLocationData.Builder()
                    .accuracy(bdLocation.getRadius())//getRadius 获取定位精度,默认值0.0f
                    .latitude(mLatitude)//百度纬度坐标
                    .longitude(mLongitude)//百度经度坐标
                    .build();
            //设置定位数据, 只有先允许定位图层后设置数据才会生效，参见 setMyLocationEnabled(boolean)
            mBaiduMap.setMyLocationData(data);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    StringBuilder currentPosition = new StringBuilder();
                    currentPosition.append("纬度：").append(bdLocation.getLatitude()).
                            append("\n");
                    currentPosition.append("经线:").append(bdLocation.getLongitude()).
                            append("\n");
                    currentPosition.append("国家:").append(bdLocation.getCountry()).
                            append("\n");
                    currentPosition.append("省:").append(bdLocation.getProvince()).
                            append("\n");
                    currentPosition.append("市:").append(bdLocation.getCity()).
                            append("\n");
                    currentPosition.append("区:").append(bdLocation.getDistrict()).
                            append("\n");
                    currentPosition.append("街道:").append(bdLocation.getStreet())
                            .append(bdLocation.getStreetNumber())
                            .append("\n");
                    currentPosition.append("当前设备厂商型号:" + DeviceUtils.getManufacturer() + "  " + DeviceUtils.getModel() + " ")
                            .append("\n");
//                    currentPosition.append("当前IP地址："+ NetworkUtils.getIPAddress(true))
//                            .append("\n");
                    currentPosition.append("当前海拔高度:").append(bdLocation.getAltitude() + "米")
                            .append("\n");
                    currentPosition.append("定位方式:");
                    if (bdLocation.getLocType() == BDLocation.TypeGpsLocation) {
                        currentPosition.append("GPS");
                    } else if (bdLocation.getLocType() == BDLocation.TypeNetWorkLocation) {
                        currentPosition.append("网络");
                    }

                    positionText.setText(currentPosition);
                }
            });

        }
    }

    private void navigateTo(BDLocation bdLocation) {
       if (isFirstLocate){
           LatLng ll = new LatLng(bdLocation.getLatitude(),bdLocation.getLongitude());
           MapStatusUpdate update = MapStatusUpdateFactory.newLatLng(ll);
           mBaiduMap.animateMapStatus(update);
           update = MapStatusUpdateFactory.zoomTo(16f);
           mBaiduMap.animateMapStatus(update);
           isFirstLocate = false;
       }

       MyLocationData.Builder builder = new MyLocationData.Builder();
       builder.latitude(bdLocation.getLatitude());
       builder.longitude(bdLocation.getLongitude());
        MyLocationData myLocationData = builder.build();
        mBaiduMap.setMyLocationData(myLocationData);

    }
}
