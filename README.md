# Update-Module-ForOneClass
实现的功能

用一个类封装了更新模块的基本功能


调用方式

1、把YougelUpdate这个类复制到工程目录下

2、实例化YougelUpdate对象

3、调用checkVersion方法


用例

YougelUpdate update=new YougelUpdate(MainActivity.this);

update.checkVersion(getString(R.string.updateUrl));

在String.xml中

<string name="updateUrl">http://192.168.1.102:8080/mashen/version.txt</string>

这是网络请求的地址获取version.txt，里面是json格式的

version.txt如下：

{

	"versionCode":2,
  
	"versionName":"1.1",
  
	"downloadUrl":"http://192.168.1.102:8080/mashen/update.apk",
  
	"description":"update message"
  
}

这个网络请求地址根据自身实际决定，只有能访问即可，至于json解析里面的代码在checkVersion方法中，可以自行修改

最后在AndroidManifest.xml文件中要添加访问网络权限
