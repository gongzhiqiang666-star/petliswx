/**
 * 宠物卡片组件
 * 显示宠物信息，包括自动计算的年龄
 */
const ageCalculator = require('../../utils/age-calculator.js');

Component({
  properties: {
    pet: {
      type: Object,
      value: {}
    },
    selected: {
      type: Boolean,
      value: false
    }
  },

  data: {
    age: '未知'
  },

  observers: {
    'pet.birthday': function(birthday) {
      this.setData({
        age: ageCalculator.calculateAge(birthday)
      });
    }
  },

  methods: {
    onTap() {
      this.triggerEvent('tap', {
        pet: this.properties.pet
      });
    }
  }
})
