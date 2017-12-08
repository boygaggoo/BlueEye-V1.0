## Blue Eye
蔚蓝之眼，远届科技一款水下摄像头产品。该项目是产品的移动APP，运行于安卓系统，最低支持安卓5.0。
***
### Author: Eli Chang
### E-mail: eliflichang@gmail.com
***
### 主要功能
* [实现对水下设备的控制，包括设备移动，视频清晰度切换等](#ui)

* [播放来自水下设备的高清视频](#ui_1)

* [截取和录制正在播放的视频，并保存到本地](#ui_2)

* [一键分享保存的文件](#ui_3)

---
|Logo|下载地址|
|---|---|
|![logo](/app/src/main/res/mipmap-xhdpi/logo.png "logo")|https://www.coolapk.com/apk/162501|

___

## 设备控制
> #### UI
> ![img](/imgs/Screenshot_2017-11-14-16-21-23-478_com.ubi.blueeye.png "img")
> #### 使用方法
>> 1. 线控：控制摄像头入水深度
>> 2. 亮度调节：调节摄像头LED补光
>> 3. 分辨率切换：切换摄像头清晰度
>> 4. 移动控制：控制设备移动
> #### 实现方式
>> 设备使用Socket搭建服务器，移动端连接并发送控制数据
>> 
## 视频播放
> #### UI
> ![img](/imgs/Screenshot_2017-11-14-16-19-45-318_com.ubi.blueeye.png "img")
> #### 使用方法
>> 打开应用即尝试连接
> #### 实现方式
>> 使用vlc-android实现该功能
>> 
## 截屏、录屏
> #### UI
> ![img](/imgs/Screenshot_2017-11-14-16-30-54-631_com.ubi.blueeye.png "img")
> #### 使用方法
>> 1. 点击圆形按钮实现截屏
>> 2. 长按0.5秒开始录屏，离开后停止录屏、
> #### 实现方式
>> 使用MediaProjection实现(Android 5.0及以上)
## 分享
> #### UI
> ![img](/imgs/Screenshot_2017-11-14-16-21-13-300_com.ubi.blueeye.png "img")
> #### 使用方法
>> 1. 在文件预览窗口长按或点击省略号，弹出操作页面，点击分享，选择分享平台
>> 2. 竖屏下，长按某文件(可多选)，点击分享，选择分享平台
> #### 实现方式
>> 使用腾讯、新浪开放SDK实现(目前可分享到新浪微博、微信好友，朋友圈，QQ好友，QQ空间)