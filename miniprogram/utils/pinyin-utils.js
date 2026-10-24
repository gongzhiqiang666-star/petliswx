/**
 * 拼音转换工具（简化版）
 * 只处理常用汉字的拼音首字母
 */

// 常用汉字拼音首字母映射表（仅示例，实际使用需要更完整的映射）
const PINYIN_FIRST_LETTER = {
  // A-D
  '阿': 'A', '爱': 'A', '安': 'A', '矮': 'A',
  '八': 'B', '巴': 'B', '白': 'B', '百': 'B', '比': 'B', '博': 'B', '布': 'B',
  '才': 'C', '草': 'C', '茶': 'C', '长': 'C', '成': 'C', '出': 'C', '川': 'C', '春': 'C', '刺': 'C', '翠': 'C',
  '大': 'D', '代': 'D', '丹': 'D', '德': 'D', '得': 'D', '地': 'D', '第': 'D', '东': 'D', '都': 'D',
  // E-H
  '鹅': 'E', '而': 'E',
  '发': 'F', '法': 'F', '反': 'F', '飞': 'F', '非': 'F', '费': 'F', '芬': 'F', '风': 'F', '福': 'F',
  '嘎': 'G', '改': 'G', '感': 'G', '刚': 'G', '高': 'G', '哥': 'G', '格': 'G', '公': 'G', '古': 'G', '关': 'G', '龟': 'G', '贵': 'G', '国': 'G', '果': 'G',
  '哈': 'H', '海': 'H', '含': 'H', '汉': 'H', '河': 'H', '黑': 'H', '红': 'H', '狐': 'H', '虎': 'H', '花': 'H', '华': 'H', '环': 'H', '黄': 'H', '灰': 'H', '火': 'H',
  // I-L
  '吉': 'J', '金': 'J', '京': 'J', '九': 'J', '菊': 'J',
  '卡': 'K', '开': 'K', '康': 'K', '考': 'K', '柯': 'K', '可': 'K', '克': 'K', '肯': 'K', '枯': 'K', '库': 'K', '快': 'K',
  '拉': 'L', '来': 'L', '蓝': 'L', '老': 'L', '雷': 'L', '狸': 'L', '丽': 'L', '利': 'L', '龙': 'L', '芦': 'L', '鹿': 'L', '罗': 'L', '骆': 'L',
  // M-P
  '马': 'M', '猫': 'M', '美': 'M', '门': 'M', '孟': 'M', '咪': 'M', '秘': 'M', '绵': 'M', '苗': 'M', '摩': 'M', '墨': 'M', '母': 'M', '木': 'M',
  '那': 'N', '南': 'N', '尼': 'N', '牛': 'N', '纽': 'N', '农': 'N',
  '欧': 'O',
  '爬': 'P', '帕': 'P', '胖': 'P', '彭': 'P', '皮': 'P', '平': 'P', '珀': 'P', '蒲': 'P', '普': 'P',
  // Q-T
  '七': 'Q', '奇': 'Q', '其': 'Q', '青': 'Q', '清': 'Q', '球': 'Q', '犬': 'Q', '全': 'Q',
  '热': 'R', '绒': 'R', '乳': 'R',
  '萨': 'S', '三': 'S', '色': 'S', '山': 'S', '闪': 'S', '尚': 'S', '蛇': 'S', '狮': 'S', '石': 'S', '松': 'S', '苏': 'S',
  '它': 'T', '太': 'T', '泰': 'T', '唐': 'T', '特': 'T', '天': 'T', '土': 'T', '兔': 'T', '吐': 'T',
  // U-X
  '娃': 'W', '外': 'W', '王': 'W', '威': 'W', '文': 'W', '乌': 'W', '无': 'W', '五': 'W', '西': 'X', '喜': 'X', '虾': 'X', '小': 'X', '蟹': 'X', '熊': 'X', '雪': 'X',
  // Y-Z
  '鸭': 'Y', '亚': 'Y', '燕': 'Y', '羊': 'Y', '野': 'Y', '银': 'Y', '印': 'Y', '英': 'Y', '鹦': 'Y', '鹰': 'Y', '鱼': 'Y', '云': 'Y',
  '藏': 'Z', '獴': 'M', '招': 'Z', '珍': 'Z', '猪': 'Z', '竹': 'Z', '爪': 'Z', '转': 'Z', '壮': 'Z', '钻': 'Z'
};

/**
 * 获取汉字的拼音首字母
 * @param {String} char - 单个汉字
 * @returns {String} 拼音首字母（大写）
 */
function getPinyinFirstLetter(char) {
  // 如果是英文字母，直接返回大写
  if (/^[a-zA-Z]$/.test(char)) {
    return char.toUpperCase();
  }

  // 查找映射表
  if (PINYIN_FIRST_LETTER[char]) {
    return PINYIN_FIRST_LETTER[char];
  }

  // 如果没有找到，使用Unicode编码范围来判断（简化版）
  const code = char.charCodeAt(0);
  if (code >= 0x4E00 && code <= 0x9FA5) {
    // 中文字符范围，根据Unicode编码分段（非常粗略）
    if (code >= 0x4E00 && code <= 0x52F7) return 'A';
    if (code >= 0x52F8 && code <= 0x57EF) return 'B';
    if (code >= 0x57F0 && code <= 0x5CE7) return 'C';
    if (code >= 0x5CE8 && code <= 0x61Df) return 'D';
    if (code >= 0x61E0 && code <= 0x6617) return 'E';
    if (code >= 0x6618 && code <= 0x6A4F) return 'F';
    if (code >= 0x6A50 && code <= 0x6E87) return 'G';
    if (code >= 0x6E88 && code <= 0x72BF) return 'H';
    if (code >= 0x72C0 && code <= 0x76F7) return 'J';
    if (code >= 0x76F8 && code <= 0x7B2F) return 'K';
    if (code >= 0x7B30 && code <= 0x7F67) return 'L';
    if (code >= 0x7F68 && code <= 0x83AF) return 'M';
    if (code >= 0x83B0 && code <= 0x87F7) return 'N';
    if (code >= 0x87F8 && code <= 0x8C2F) return 'O';
    if (code >= 0x8C30 && code <= 0x9067) return 'P';
    if (code >= 0x9068 && code <= 0x949F) return 'Q';
    if (code >= 0x94A0 && code >= 0x98D7) return 'R';
    if (code >= 0x98D8 && code <= 0x9D0F) return 'S';
    if (code >= 0x9D10 && code <= 0xA147) return 'T';
    if (code >= 0xA148 && code <= 0xA587) return 'W';
    if (code >= 0xA588 && code <= 0xA9C7) return 'X';
    if (code >= 0xA9C8 && code <= 0xAE07) return 'Y';
    if (code >= 0xAE08 && code <= 0xB247) return 'Z';
  }

  // 默认返回 '#'，表示未知
  return '#';
}

/**
 * 获取字符串的拼音首字母
 * @param {String} str - 字符串
 * @returns {String} 拼音首字母（大写）
 */
function getFirstLetter(str) {
  if (!str || str.length === 0) {
    return '#';
  }

  const firstChar = str.charAt(0);
  return getPinyinFirstLetter(firstChar);
}

/**
 * 将数据按首字母分组
 * @param {Array} items - 数据项数组
 * @param {String} nameKey - 名称字段键名
 * @returns {Array} 分组后的数据 [{ letter: 'A', items: [...] }]
 */
function groupByFirstLetter(items, nameKey = 'name') {
  const grouped = {};

  items.forEach(item => {
    const name = item[nameKey] || '';
    const letter = getFirstLetter(name);

    if (!grouped[letter]) {
      grouped[letter] = [];
    }

    grouped[letter].push(item);
  });

  // 转换为数组并按字母排序
  const result = Object.keys(grouped)
    .sort()
    .map(letter => ({
      letter,
      items: grouped[letter]
    }));

  return result;
}

module.exports = {
  getFirstLetter,
  getPinyinFirstLetter,
  groupByFirstLetter
};
