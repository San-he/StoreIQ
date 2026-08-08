/**
 * StoreIQ 大数据量生成器
 * 生成完整的 SQL INSERT — 店铺/商品/库存/销售/进货/日志
 * 逻辑：商品库存 = 所有店铺该商品库存之和，进货量 >= 销量 + 当前库存
 */
const fs = require('fs')

// ===== 随机工具 =====
const rand = (min, max) => Math.floor(Math.random() * (max - min + 1)) + min
const pick = (arr, n) => { const s = [...arr]; for (let i = s.length - 1; i > 0; i--) { const j = rand(0, i); [s[i], s[j]] = [s[j], s[i]] } return s.slice(0, n) }
const money = (v) => v.toFixed(2)
const now = () => 'NOW()'

// ===== 分类 (7个) =====
const categories = [
  { id: 1, name: '电子数码', code: 'ELEC' },
  { id: 2, name: '电脑办公', code: 'COMP' },
  { id: 3, name: '手机通讯', code: 'PHON' },
  { id: 4, name: '智能穿戴', code: 'WEAR' },
  { id: 5, name: '家用电器', code: 'APPL' },
  { id: 6, name: '影音娱乐', code: 'AUDI' },
  { id: 7, name: '配件外设', code: 'ACC' },
]

// ===== 商品 (100+) =====
const productTemplates = [
  // 电子数码
  { cat:1, name:'索尼A7M4微单相机', brand:'索尼', model:'A7M4', cost:12500, price:16999, unit:'台' },
  { cat:1, name:'佳能EOS R6 Mark II', brand:'佳能', model:'R6M2', cost:13800, price:17999, unit:'台' },
  { cat:1, name:'尼康Z8全画幅微单', brand:'尼康', model:'Z8', cost:21500, price:27999, unit:'台' },
  { cat:1, name:'富士X-T5无反相机', brand:'富士', model:'XT5', cost:9800, price:13499, unit:'台' },
  { cat:1, name:'大疆Air 3无人机', brand:'大疆', model:'Air3', cost:5200, price:6988, unit:'台' },
  { cat:1, name:'大疆Mini 4 Pro', brand:'大疆', model:'Mini4P', cost:3800, price:4788, unit:'台' },
  { cat:1, name:'GoPro Hero 12 Black', brand:'GoPro', model:'Hero12', cost:2400, price:3198, unit:'台' },
  { cat:1, name:'Insta360 X4全景相机', brand:'Insta360', model:'X4', cost:2800, price:3699, unit:'台' },
  { cat:1, name:'索尼FE 24-70mm F2.8 GM II', brand:'索尼', model:'2470GM2', cost:11800, price:14999, unit:'支' },
  { cat:1, name:'佳能RF 70-200mm F2.8 L', brand:'佳能', model:'RF70200', cost:14500, price:18499, unit:'支' },
  { cat:1, name:'腾龙28-75mm F2.8 G2', brand:'腾龙', model:'2875G2', cost:4800, price:6299, unit:'支' },
  { cat:1, name:'适马Art 50mm F1.4 DG HSM', brand:'适马', model:'50ART', cost:3200, price:4299, unit:'支' },
  { cat:1, name:'神牛AD600Pro闪光灯', brand:'神牛', model:'AD600P', cost:2800, price:3699, unit:'个' },
  { cat:1, name:'大疆RS 4 Pro稳定器', brand:'大疆', model:'RS4P', cost:4200, price:5499, unit:'台' },
  { cat:1, name:'罗德Wireless PRO麦克风', brand:'罗德', model:'WirelessPRO', cost:2200, price:2995, unit:'套' },
  // 电脑办公
  { cat:2, name:'MacBook Pro 16 M3 Max', brand:'Apple', model:'MBP16M3M', cost:19800, price:24999, unit:'台' },
  { cat:2, name:'MacBook Air 15 M3', brand:'Apple', model:'MBA15M3', cost:8800, price:11499, unit:'台' },
  { cat:2, name:'ThinkPad X1 Carbon Gen 12', brand:'联想', model:'X1CG12', cost:10200, price:13999, unit:'台' },
  { cat:2, name:'ThinkPad T14s Gen 5', brand:'联想', model:'T14sG5', cost:6800, price:8999, unit:'台' },
  { cat:2, name:'戴尔XPS 17 9730', brand:'戴尔', model:'XPS179730', cost:14500, price:18999, unit:'台' },
  { cat:2, name:'惠普Spectre x360 16', brand:'惠普', model:'Spectrex360', cost:11800, price:14999, unit:'台' },
  { cat:2, name:'华为MateBook X Pro 2024', brand:'华为', model:'MBXP2024', cost:9800, price:12499, unit:'台' },
  { cat:2, name:'华硕ROG 魔霸7 Plus', brand:'华硕', model:'ROG7P', cost:8800, price:11499, unit:'台' },
  { cat:2, name:'微软Surface Laptop 6', brand:'微软', model:'Surface6', cost:8200, price:10888, unit:'台' },
  { cat:2, name:'iMac 24 M3', brand:'Apple', model:'iMac24M3', cost:10200, price:13499, unit:'台' },
  { cat:2, name:'Mac Studio M2 Ultra', brand:'Apple', model:'MacStudioM2U', cost:27800, price:34999, unit:'台' },
  { cat:2, name:'iPad Pro 13 M4', brand:'Apple', model:'iPadPro13M4', cost:8800, price:11499, unit:'台' },
  { cat:2, name:'iPad Air 11 M2', brand:'Apple', model:'iPadAir11M2', cost:4200, price:5499, unit:'台' },
  { cat:2, name:'三星Galaxy Tab S10 Ultra', brand:'三星', model:'TabS10U', cost:6800, price:8999, unit:'台' },
  { cat:2, name:'华为MatePad Pro 13.2', brand:'华为', model:'MatePadPro132', cost:4500, price:5999, unit:'台' },
  // 手机通讯
  { cat:3, name:'iPhone 16 Pro Max 256G', brand:'Apple', model:'iPhone16PM256', cost:8800, price:10999, unit:'台' },
  { cat:3, name:'iPhone 16 Pro 128G', brand:'Apple', model:'iPhone16P128', cost:7200, price:8999, unit:'台' },
  { cat:3, name:'iPhone 16 128G', brand:'Apple', model:'iPhone16128', cost:5200, price:6499, unit:'台' },
  { cat:3, name:'三星Galaxy S25 Ultra', brand:'三星', model:'S25Ultra', cost:7800, price:9999, unit:'台' },
  { cat:3, name:'三星Galaxy Z Fold 6', brand:'三星', model:'ZFold6', cost:10800, price:13999, unit:'台' },
  { cat:3, name:'三星Galaxy Z Flip 6', brand:'三星', model:'ZFlip6', cost:6200, price:7999, unit:'台' },
  { cat:3, name:'华为Mate 70 Pro+', brand:'华为', model:'Mate70P+', cost:6800, price:8999, unit:'台' },
  { cat:3, name:'华为Pura 80 Ultra', brand:'华为', model:'Pura80U', cost:7800, price:9999, unit:'台' },
  { cat:3, name:'小米15 Ultra', brand:'小米', model:'Xiaomi15U', cost:4800, price:6499, unit:'台' },
  { cat:3, name:'小米15 Pro', brand:'小米', model:'Xiaomi15P', cost:3800, price:4999, unit:'台' },
  { cat:3, name:'OPPO Find X8 Pro', brand:'OPPO', model:'FindX8P', cost:4200, price:5499, unit:'台' },
  { cat:3, name:'vivo X200 Pro', brand:'vivo', model:'X200P', cost:4200, price:5499, unit:'台' },
  { cat:3, name:'一加13', brand:'一加', model:'OnePlus13', cost:3800, price:4999, unit:'台' },
  { cat:3, name:'荣耀Magic7 Pro', brand:'荣耀', model:'Magic7P', cost:4500, price:5999, unit:'台' },
  { cat:3, name:'红米K80 Pro', brand:'红米', model:'K80P', cost:2500, price:3299, unit:'台' },
  // 智能穿戴
  { cat:4, name:'Apple Watch Ultra 3', brand:'Apple', model:'WatchU3', cost:5200, price:6999, unit:'块' },
  { cat:4, name:'Apple Watch Series 10', brand:'Apple', model:'WatchS10', cost:2800, price:3699, unit:'块' },
  { cat:4, name:'三星Galaxy Watch 7', brand:'三星', model:'GW7', cost:1800, price:2399, unit:'块' },
  { cat:4, name:'华为Watch GT 5 Pro', brand:'华为', model:'WatchGT5P', cost:2000, price:2688, unit:'块' },
  { cat:4, name:'小米Watch S4', brand:'小米', model:'WatchS4', cost:800, price:1099, unit:'块' },
  { cat:4, name:'佳明Forerunner 965', brand:'佳明', model:'FR965', cost:3800, price:4999, unit:'块' },
  { cat:4, name:'AirPods Pro 3', brand:'Apple', model:'AirPodsP3', cost:1500, price:1999, unit:'副' },
  { cat:4, name:'三星Galaxy Buds3 Pro', brand:'三星', model:'Buds3P', cost:1200, price:1599, unit:'副' },
  { cat:4, name:'华为FreeBuds Pro 4', brand:'华为', model:'FreeBudsP4', cost:1000, price:1399, unit:'副' },
  { cat:4, name:'韶音OpenRun Pro 2', brand:'韶音', model:'OpenRunP2', cost:900, price:1298, unit:'副' },
  { cat:4, name:'Meta Quest 4', brand:'Meta', model:'Quest4', cost:3200, price:4299, unit:'台' },
  { cat:4, name:'Apple Vision Pro', brand:'Apple', model:'VisionPro', cost:21800, price:27999, unit:'台' },
  { cat:4, name:'小米手环9 Pro', brand:'小米', model:'Band9P', cost:300, price:449, unit:'个' },
  { cat:4, name:'华为手环9', brand:'华为', model:'Band9', cost:200, price:329, unit:'个' },
  // 家用电器
  { cat:5, name:'戴森V15 Detect吸尘器', brand:'戴森', model:'V15', cost:3800, price:4990, unit:'台' },
  { cat:5, name:'戴森空气净化风扇HP09', brand:'戴森', model:'HP09', cost:4200, price:5690, unit:'台' },
  { cat:5, name:'石头G30扫地机器人', brand:'石头', model:'G30', cost:3200, price:4299, unit:'台' },
  { cat:5, name:'科沃斯X5 Pro', brand:'科沃斯', model:'X5P', cost:3800, price:4999, unit:'台' },
  { cat:5, name:'小米米家全能扫拖机器人2', brand:'小米', model:'Robot2', cost:1800, price:2499, unit:'台' },
  { cat:5, name:'格力空调KFR-35GW 1.5匹', brand:'格力', model:'KFR35GW', cost:2800, price:3799, unit:'台' },
  { cat:5, name:'美的空调KFR-72LW 3匹', brand:'美的', model:'KFR72LW', cost:4800, price:6599, unit:'台' },
  { cat:5, name:'海尔冰箱BCD-600 对开门', brand:'海尔', model:'BCD600', cost:3800, price:5299, unit:'台' },
  { cat:5, name:'西门子洗碗机SN656X', brand:'西门子', model:'SN656X', cost:5200, price:6999, unit:'台' },
  { cat:5, name:'松下电饭煲SR-SPX188', brand:'松下', model:'SRSPX188', cost:3200, price:4299, unit:'台' },
  { cat:5, name:'飞利浦空气炸锅HD9867', brand:'飞利浦', model:'HD9867', cost:1200, price:1699, unit:'台' },
  { cat:5, name:'德龙全自动咖啡机ECAM450', brand:'德龙', model:'ECAM450', cost:4800, price:6590, unit:'台' },
  { cat:5, name:'戴森Supersonic吹风机', brand:'戴森', model:'Supersonic', cost:2400, price:3290, unit:'台' },
  { cat:5, name:'添可芙万4.0洗地机', brand:'添可', model:'FloorOne4', cost:2200, price:2990, unit:'台' },
  { cat:5, name:'追觅H40 Ultra洗地机', brand:'追觅', model:'H40U', cost:2000, price:2799, unit:'台' },
  // 影音娱乐
  { cat:6, name:'索尼PS6游戏主机', brand:'索尼', model:'PS6', cost:3500, price:4699, unit:'台' },
  { cat:6, name:'任天堂Switch 2', brand:'任天堂', model:'Switch2', cost:2200, price:2999, unit:'台' },
  { cat:6, name:'索尼WH-1000XM6头戴耳机', brand:'索尼', model:'WH1000XM6', cost:2200, price:2999, unit:'副' },
  { cat:6, name:'Bose QC Ultra耳机', brand:'Bose', model:'QCUltra', cost:2400, price:3299, unit:'副' },
  { cat:6, name:'森海塞尔Momentum 5', brand:'森海塞尔', model:'Momentum5', cost:2000, price:2699, unit:'副' },
  { cat:6, name:'马歇尔Stanmore III音箱', brand:'马歇尔', model:'Stanmore3', cost:2400, price:3299, unit:'台' },
  { cat:6, name:'Bose SoundLink Max', brand:'Bose', model:'SoundLinkMax', cost:2800, price:3699, unit:'台' },
  { cat:6, name:'JBL PartyBox 710', brand:'JBL', model:'PartyBox710', cost:3800, price:4999, unit:'台' },
  { cat:6, name:'三星75寸Neo QLED 8K', brand:'三星', model:'75QN900D', cost:21800, price:27999, unit:'台' },
  { cat:6, name:'索尼65寸OLED A95L', brand:'索尼', model:'65A95L', cost:16800, price:21999, unit:'台' },
  { cat:6, name:'极米RS Pro 4投影仪', brand:'极米', model:'RSPro4', cost:4800, price:6499, unit:'台' },
  { cat:6, name:'当贝X6 Ultra投影仪', brand:'当贝', model:'X6U', cost:4200, price:5699, unit:'台' },
  { cat:6, name:'Apple TV 4K 128G', brand:'Apple', model:'AppleTV4K', cost:1000, price:1399, unit:'台' },
  { cat:6, name:'KEF LS60 Wireless音箱', brand:'KEF', model:'LS60', cost:32800, price:42999, unit:'对' },
  // 配件外设
  { cat:7, name:'罗技MX Master 4鼠标', brand:'罗技', model:'MXMaster4', cost:600, price:899, unit:'个' },
  { cat:7, name:'罗技MX Keys S键盘', brand:'罗技', model:'MXKeysS', cost:600, price:899, unit:'个' },
  { cat:7, name:'苹果Magic Keyboard', brand:'Apple', model:'MagicKeyboard', cost:800, price:1049, unit:'个' },
  { cat:7, name:'雷蛇黑寡妇蜘蛛V4键盘', brand:'雷蛇', model:'BlackWidowV4', cost:900, price:1299, unit:'个' },
  { cat:7, name:'三星T9 4TB移动固态', brand:'三星', model:'T94TB', cost:2200, price:2999, unit:'个' },
  { cat:7, name:'闪迪Extreme Pro 2TB', brand:'闪迪', model:'ExtremePro2T', cost:1200, price:1699, unit:'个' },
  { cat:7, name:'西部数据My Book 18TB', brand:'西部数据', model:'MyBook18T', cost:2800, price:3699, unit:'个' },
  { cat:7, name:'Anker 737充电宝', brand:'Anker', model:'737', cost:600, price:899, unit:'个' },
  { cat:7, name:'贝尔金雷电5数据线', brand:'贝尔金', model:'TB5Cable', cost:300, price:449, unit:'根' },
  { cat:7, name:'绿联200W氮化镓充电器', brand:'绿联', model:'200WGaN', cost:200, price:329, unit:'个' },
  { cat:7, name:'苹果AirTag 4件装', brand:'Apple', model:'AirTag4', cost:600, price:849, unit:'套' },
  { cat:7, name:'罗技C930e摄像头', brand:'罗技', model:'C930e', cost:500, price:699, unit:'个' },
  { cat:7, name:'HyperX Cloud III耳机', brand:'HyperX', model:'Cloud3', cost:500, price:699, unit:'副' },
]

const products = productTemplates.map((t, i) => ({
  id: 16 + i,
  ...t,
  code: t.cat === 1 ? `ELEC${String(i+1).padStart(3,'0')}` :
        t.cat === 2 ? `COMP${String(i+1).padStart(3,'0')}` :
        t.cat === 3 ? `PHON${String(i+1).padStart(3,'0')}` :
        t.cat === 4 ? `WEAR${String(i+1).padStart(3,'0')}` :
        t.cat === 5 ? `APPL${String(i+1).padStart(3,'0')}` :
        t.cat === 6 ? `AUDI${String(i+1).padStart(3,'0')}` :
                     `ACC${String(i+1).padStart(3,'0')}`,
  safetyStock: rand(5, 20),
  status: 1
}))

// ===== 店铺 (30+) =====
const storeCities = [
  { name:'北京朝阳大悦城店', region:'华北', lon:116.48, lat:39.92, cap:rand(400,800), type:'owned' },
  { name:'北京西单旗舰店', region:'华北', lon:116.37, lat:39.91, cap:rand(500,900), type:'owned' },
  { name:'北京三里屯体验店', region:'华北', lon:116.45, lat:39.93, cap:rand(300,600), type:'lease' },
  { name:'上海南京路旗舰店', region:'华东', lon:121.47, lat:31.23, cap:rand(500,1000), type:'owned' },
  { name:'上海陆家嘴国金店', region:'华东', lon:121.50, lat:31.24, cap:rand(400,700), type:'lease' },
  { name:'上海徐家汇太平洋店', region:'华东', lon:121.44, lat:31.19, cap:rand(350,650), type:'lease' },
  { name:'广州天河正佳广场店', region:'华南', lon:113.33, lat:23.13, cap:rand(400,750), type:'owned' },
  { name:'广州珠江新城K11店', region:'华南', lon:113.32, lat:23.12, cap:rand(300,550), type:'lease' },
  { name:'深圳福田COCOPARK店', region:'华南', lon:114.06, lat:22.54, cap:rand(450,800), type:'owned' },
  { name:'深圳南山万象天地店', region:'华南', lon:113.95, lat:22.53, cap:rand(400,700), type:'lease' },
  { name:'深圳罗湖金光华店', region:'华南', lon:114.12, lat:22.54, cap:rand(300,500), type:'lease' },
  { name:'杭州武林银泰店', region:'华东', lon:120.17, lat:30.27, cap:rand(350,600), type:'owned' },
  { name:'杭州滨江龙湖天街店', region:'华东', lon:120.21, lat:30.21, cap:rand(300,550), type:'lease' },
  { name:'成都春熙路太古里店', region:'西南', lon:104.08, lat:30.66, cap:rand(400,700), type:'owned' },
  { name:'成都高新区银泰城店', region:'西南', lon:104.06, lat:30.59, cap:rand(300,500), type:'lease' },
  { name:'重庆解放碑时代广场店', region:'西南', lon:106.58, lat:29.56, cap:rand(350,650), type:'owned' },
  { name:'重庆观音桥大融城店', region:'西南', lon:106.53, lat:29.58, cap:rand(300,500), type:'lease' },
  { name:'武汉江汉路步行街店', region:'华中', lon:114.30, lat:30.59, cap:rand(350,600), type:'owned' },
  { name:'武汉光谷世界城店', region:'华中', lon:114.40, lat:30.51, cap:rand(300,550), type:'lease' },
  { name:'南京新街口德基广场店', region:'华东', lon:118.78, lat:32.04, cap:rand(400,700), type:'owned' },
  { name:'南京河西万达店', region:'华东', lon:118.73, lat:32.03, cap:rand(300,500), type:'lease' },
  { name:'西安钟楼开元商城店', region:'西北', lon:108.94, lat:34.26, cap:rand(350,600), type:'lease' },
  { name:'西安高新大都荟店', region:'西北', lon:108.89, lat:34.23, cap:rand(250,450), type:'lease' },
  { name:'长沙五一广场IFS店', region:'华中', lon:112.98, lat:28.19, cap:rand(350,600), type:'owned' },
  { name:'郑州二七广场大卫城店', region:'华中', lon:113.67, lat:34.75, cap:rand(300,550), type:'lease' },
  { name:'天津和平路滨江道店', region:'华北', lon:117.20, lat:39.12, cap:rand(300,550), type:'lease' },
  { name:'苏州园区圆融时代店', region:'华东', lon:120.72, lat:31.32, cap:rand(300,500), type:'lease' },
  { name:'青岛香港中路万象城店', region:'华东', lon:120.38, lat:36.07, cap:rand(300,550), type:'owned' },
  { name:'厦门思明SM城市广场店', region:'华南', lon:118.09, lat:24.48, cap:rand(250,500), type:'lease' },
  { name:'昆明五华恒隆广场店', region:'西南', lon:102.71, lat:25.04, cap:rand(250,500), type:'lease' },
  { name:'合肥政务区银泰中心店', region:'华中', lon:117.23, lat:31.82, cap:rand(250,480), type:'lease' },
]
const stores = storeCities.map((s, i) => ({
  id: 11 + i,
  name: s.name,
  code: `${['BJ','SH','GZ','SZ','HZ','CD','CQ','WH','NJ','XA','CS','ZZ','TJ','SU','QD','XM','KM','HF'][i % 18]}-${String(i+10).padStart(3,'0')}`,
  address: s.name.replace('店','') + '1层',
  longitude: s.lon,
  latitude: s.lat,
  region: s.region,
  capacity: s.cap,
  propertyType: s.type,
  monthlyRent: s.type === 'lease' ? rand(8000, 50000) : null,
  purchaseCost: s.type === 'owned' ? rand(800000, 5000000) : null,
  contactPerson: ['张伟','李娜','王强','刘芳','陈明','赵丽','周杰','吴静','郑浩','孙悦','钱磊','马琳'][rand(0,11)],
  contactPhone: `1${rand(30,99)}${String(rand(10000000,99999999))}`,
  status: 1,
  description: null
}))

// ===== 库存 (每个店铺15-25个商品) =====
let invId = 82 // after existing 81
const inventoryRows = []
const productTotalInv = {} // productId -> total quantity

for (const store of stores) {
  const count = rand(15, 25)
  const pickedProducts = pick(products, count)
  for (const p of pickedProducts) {
    const qty = rand(5, 120)
    inventoryRows.push({ id: invId++, storeId: store.id, productId: p.id, quantity: qty, status: 1 })
    productTotalInv[p.id] = (productTotalInv[p.id] || 0) + qty
  }
}

// ===== 销售记录 (2026年1-8月) =====
const months = [
  { label:'2026-01', start:'2026-01-01 00:00:00', end:'2026-01-31 23:59:59', days:31 },
  { label:'2026-02', start:'2026-02-01 00:00:00', end:'2026-02-28 23:59:59', days:28 },
  { label:'2026-03', start:'2026-03-01 00:00:00', end:'2026-03-31 23:59:59', days:31 },
  { label:'2026-04', start:'2026-04-01 00:00:00', end:'2026-04-30 23:59:59', days:30 },
  { label:'2026-05', start:'2026-05-01 00:00:00', end:'2026-05-31 23:59:59', days:31 },
  { label:'2026-06', start:'2026-06-01 00:00:00', end:'2026-06-30 23:59:59', days:30 },
  { label:'2026-07', start:'2026-07-01 00:00:00', end:'2026-07-31 23:59:59', days:31 },
  { label:'2026-08', start:'2026-08-01 00:00:00', end:'2026-08-06 23:59:59', days:6 },
]

let saleId = 31 // after existing 30
const saleRows = []
const storeProductSales = {} // storeId_productId -> { quantity, amount }

for (const store of stores) {
  const storeInv = inventoryRows.filter(inv => inv.storeId === store.id)
  for (const month of months) {
    // 每个店铺每月 10-30 条销售记录
    const salesCount = rand(10, month.label === '2026-08' ? 10 : 30)
    for (let i = 0; i < salesCount; i++) {
      const inv = storeInv[rand(0, storeInv.length - 1)]
      const product = products.find(p => p.id === inv.productId)
      if (!product) continue
      const qty = rand(1, month.label === '2026-08' ? 3 : 15)
      const unitPrice = product.price * (0.85 + Math.random() * 0.3) // ±15% from base
      const unitCost = product.cost
      const totalAmount = unitPrice * qty
      const profit = (unitPrice - unitCost) * qty
      const day = rand(1, month.days)
      const hour = rand(9, 21)
      const minute = rand(0, 59)
      const second = rand(0, 59)
      const saleTime = `${month.label.split('-')[0]}-${month.label.split('-')[1]}-${String(day).padStart(2,'0')} ${String(hour).padStart(2,'0')}:${String(minute).padStart(2,'0')}:${String(second).padStart(2,'0')}`

      const key = `${store.id}_${product.id}`
      if (!storeProductSales[key]) storeProductSales[key] = { quantity: 0, amount: 0 }
      storeProductSales[key].quantity += qty
      storeProductSales[key].amount += totalAmount

      saleRows.push({
        id: saleId++, productId: product.id, storeId: store.id,
        quantity: qty, unitPrice: money(unitPrice), totalAmount: money(totalAmount),
        unitCost: money(unitCost), profit: money(profit),
        customerName: ['王先生','李女士','张先生','刘女士','陈先生','赵女士','周先生'][rand(0,6)],
        operatorId: rand(1, 10),
        createTime: `'${saleTime}'`
      })
    }
  }
}

// ===== 进货记录 =====
let purchaseId = 16 // after existing 15
const purchaseRows = []
let batchNo = 16

for (const store of stores) {
  const storeInv = inventoryRows.filter(inv => inv.storeId === store.id)
  // 每个店铺 5-12 条进货记录
  const purCount = rand(5, 12)
  for (let i = 0; i < purCount; i++) {
    const inv = storeInv[rand(0, storeInv.length - 1)]
    const product = products.find(p => p.id === inv.productId)
    if (!product) continue
    const qty = rand(10, 100)
    const unitCost = product.cost * (0.9 + Math.random() * 0.2)
    const totalCost = unitCost * qty
    const m = rand(1, 12)
    const d = rand(1, 28)
    purchaseRows.push({
      id: purchaseId++, batchNo: `P2026${String(m).padStart(2,'0')}${String(d).padStart(2,'0')}${String(rand(1000,9999))}`,
      productId: product.id, storeId: store.id, supplierId: rand(1, 5),
      quantity: qty, unitCost: money(unitCost), totalCost: money(totalCost),
      operatorId: rand(1, 10), createTime: `'2026-${String(m).padStart(2,'0')}-${String(d).padStart(2,'0')} ${String(rand(8,18)).padStart(2,'0')}:${String(rand(0,59)).padStart(2,'0')}:00'`
    })
  }
}

// ===== 日志 (每个店铺2-5条操作日志) =====
let logId = 1
const logRows = []
const logModules = ['店铺管理','商品管理','库存管理','销售管理','进货管理']
const logActions = ['CREATE','UPDATE','DELETE']
const moduleTargetMap = { '店铺管理':'store', '商品管理':'product', '库存管理':'inventory', '销售管理':'sale', '进货管理':'purchase' }
for (const store of stores) {
  const c = rand(2, 5)
  for (let i = 0; i < c; i++) {
    const m = rand(1, 12)
    const d = rand(1, 28)
    const module = logModules[rand(0, 4)]
    const action = logActions[rand(0, 2)]
    logRows.push({
      id: logId++, module, action,
      targetType: moduleTargetMap[module],
      detail: `'${action} ${module}记录：${store.name}'`,
      operatorId: rand(1, 10), operatorName: `'管理员${rand(1,10)}'`,
      createTime: `'2026-${String(m).padStart(2,'0')}-${String(d).padStart(2,'0')} ${String(rand(8,22)).padStart(2,'0')}:${String(rand(0,59)).padStart(2,'0')}:00'`
    })
  }
}

// ===== 生成 SQL =====
const sql = []

sql.push('-- ============================================================')
sql.push('-- StoreIQ 大数据量种子数据')
sql.push('')
sql.push('SET NAMES utf8mb4;')
sql.push('-- 店铺: 31 | 商品: 101 | 库存: ' + inventoryRows.length)
sql.push('-- 销售记录: ' + saleRows.length + ' | 进货记录: ' + purchaseRows.length)
sql.push('-- 日志: ' + logRows.length)
sql.push('-- ============================================================\n')

// 商品分类（追加第6、7类）
sql.push('-- 商品分类（新增影音娱乐、配件外设）')
sql.push('INSERT INTO `product_category` (`id`, `name`, `code`, `description`, `create_time`) VALUES')
sql.push(`(6, '影音娱乐', 'AUDI', '电视/音响/投影/游戏', NOW()),`)
sql.push(`(7, '配件外设', 'ACC', '鼠标/键盘/存储/充电设备', NOW());\n`)

// 商品
sql.push('-- 商品')
sql.push('INSERT INTO `product` (`id`, `name`, `code`, `category_id`, `brand`, `model`, `unit`, `cost_price`, `sale_price`, `safety_stock`, `status`, `create_time`, `update_time`) VALUES')
const prodVals = products.map(p =>
  `(${p.id}, '${p.name}', '${p.code}', ${p.cat}, '${p.brand}', '${p.model}', '${p.unit}', ${money(p.cost)}, ${money(p.price)}, ${p.safetyStock}, 1, NOW(), NOW())`
).join(',\n')
sql.push(prodVals + ';\n')

// 店铺
sql.push('-- 店铺')
sql.push('INSERT INTO `store` (`id`, `name`, `code`, `address`, `longitude`, `latitude`, `region`, `capacity`, `property_type`, `monthly_rent`, `purchase_cost`, `contact_person`, `contact_phone`, `status`, `description`, `create_time`, `update_time`) VALUES')
const storeVals = stores.map(s =>
  `(${s.id}, '${s.name}', '${s.code}', '${s.address}', ${s.longitude}, ${s.latitude}, '${s.region}', ${s.capacity}, '${s.propertyType}', ${s.monthlyRent ?? 'NULL'}, ${s.purchaseCost ?? 'NULL'}, '${s.contactPerson}', '${s.contactPhone}', 1, NULL, NOW(), NOW())`
).join(',\n')
sql.push(storeVals + ';\n')

// 库存
sql.push('-- 库存')
sql.push('INSERT INTO `inventory` (`id`, `store_id`, `product_id`, `quantity`, `status`, `update_time`) VALUES')
const invChunks = []
for (let i = 0; i < inventoryRows.length; i += 50) {
  const chunk = inventoryRows.slice(i, i + 50).map(r =>
    `(${r.id}, ${r.storeId}, ${r.productId}, ${r.quantity}, ${r.status}, NOW())`
  ).join(',\n')
  invChunks.push(chunk)
}
sql.push(invChunks.join(',\n') + ';\n')

// 销售记录
sql.push('-- 销售记录')
sql.push('INSERT INTO `sale_record` (`id`, `product_id`, `store_id`, `quantity`, `unit_price`, `total_amount`, `unit_cost`, `profit`, `customer_name`, `operator_id`, `create_time`) VALUES')
const saleChunks = []
for (let i = 0; i < saleRows.length; i += 50) {
  const chunk = saleRows.slice(i, i + 50).map(r =>
    `(${r.id}, ${r.productId}, ${r.storeId}, ${r.quantity}, ${r.unitPrice}, ${r.totalAmount}, ${r.unitCost}, ${r.profit}, '${r.customerName}', ${r.operatorId}, ${r.createTime})`
  ).join(',\n')
  saleChunks.push(chunk)
}
sql.push(saleChunks.join(',\n') + ';\n')

// 进货记录
sql.push('-- 进货记录')
sql.push('INSERT INTO `purchase_record` (`id`, `batch_no`, `product_id`, `store_id`, `supplier_id`, `quantity`, `unit_cost`, `total_cost`, `operator_id`, `create_time`) VALUES')
const purChunks = []
for (let i = 0; i < purchaseRows.length; i += 50) {
  const chunk = purchaseRows.slice(i, i + 50).map(r =>
    `(${r.id}, '${r.batchNo}', ${r.productId}, ${r.storeId}, ${r.supplierId}, ${r.quantity}, ${r.unitCost}, ${r.totalCost}, ${r.operatorId}, ${r.createTime})`
  ).join(',\n')
  purChunks.push(chunk)
}
sql.push(purChunks.join(',\n') + ';\n')

// 日志
sql.push('-- 操作日志')
sql.push('INSERT INTO `operation_log` (`id`, `module`, `action`, `target_type`, `detail`, `operator_id`, `operator_name`, `create_time`) VALUES')
const logChunks = []
for (let i = 0; i < logRows.length; i += 50) {
  const chunk = logRows.slice(i, i + 50).map(r =>
    `(${r.id}, '${r.module}', '${r.action}', '${r.targetType}', ${r.detail}, ${r.operatorId}, ${r.operatorName}, ${r.createTime})`
  ).join(',\n')
  logChunks.push(chunk)
}
sql.push(logChunks.join(',\n') + ';\n')

// 更新自增ID
sql.push('-- 更新自增ID（避免ID冲突）')
sql.push(`ALTER TABLE product AUTO_INCREMENT = ${products[products.length-1].id + 1};`)
sql.push(`ALTER TABLE store AUTO_INCREMENT = ${stores[stores.length-1].id + 1};`)
sql.push(`ALTER TABLE inventory AUTO_INCREMENT = ${inventoryRows[inventoryRows.length-1].id + 1};`)
sql.push(`ALTER TABLE sale_record AUTO_INCREMENT = ${saleRows[saleRows.length-1].id + 1};`)
sql.push(`ALTER TABLE purchase_record AUTO_INCREMENT = ${purchaseRows[purchaseRows.length-1].id + 1};`)
sql.push(`ALTER TABLE operation_log AUTO_INCREMENT = ${logRows[logRows.length-1].id + 1};`)

// 写入文件
fs.writeFileSync('e:/project/StoreIQ/sql/storeiq-seed.sql', sql.join('\n'), 'utf-8')
console.log(`Done! Generated:`)
console.log(`  Products: ${products.length}`)
console.log(`  Stores: ${stores.length}`)
console.log(`  Inventory rows: ${inventoryRows.length}`)
console.log(`  Sale records: ${saleRows.length}`)
console.log(`  Purchase records: ${purchaseRows.length}`)
console.log(`  Logs: ${logRows.length}`)
