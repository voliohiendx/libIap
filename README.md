### Init
In MainActivity:
 ```xml
      override fun onCreate(savedInstanceState: Bundle?) {
            IapConnector.getInstance().initIap(this, "iap_id.json")
       }
 ```
File: iap_id.json
 ```xml
 {
   "id": "iapmonthly",
   "type": "subs"
 },
 {
   "id": "iapforever",
   "type": "inapp"
 }
]
```

### CheckPurchasesIap
 ```xml
IapConnector.getInstance().isPurchasesIap.observe(viewLifecycleOwner) {
           it?.let {
               if(it){
                   // DA MUA IAP
               } else{
                   // CHUA MUA IAP
               }
           }
       }
```

### Buy
 ```xml
IapConnector.getInstance().buyIap("iapforever")
```

### Reset Iap
 ```xml
IapConnector.getInstance().resetIap()
```

### Subscribe Success
 ```xml
IapConnector.getInstance().subscribeSuccess.observe(viewLifecycleOwner) {
          // TRA VE THONG TIN GOI IAP VUA MUA THANH CONG
}

```
### Subscribe Error
```xml
 IapConnector.getInstance().subscribeError.observe(viewLifecycleOwner) {
           // TRA VE THONG TIN LOI
 }
 ```
 
### GetAllIapProduct
```xml
 IapConnector.getInstance().getAllProductModel()
```
