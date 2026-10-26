// components/age-picker/age-picker.js
Component({
  /**
   * 组件的属性列表
   */
  properties: {
    value: {
      type: String,
      value: '',
      observer(value) {
        if (value) this.parseAge(value);
      }
    }
  },

  /**
   * 组件的初始数据
   */
  data: {
    years: [],
    months: [],
    yearIndex: 0,
    monthIndex: 0,
    // 添加12表示满12个月（1岁）
    allMonths: []
  },

  lifetimes: {
    attached() {
      // 初始化年份0-30岁
      const years = Array.from({length: 31}, (_, i) => i);
      // 初始化月份0-12（0表示0个月，1-11表示未满1岁的宠物，12表示满12个月）
      const months = Array.from({length: 13}, (_, i) => i);

      // 生成年份标签（用于picker显示）
      const yearLabels = years.map(y => `${y}岁`);
      // 生成月份标签（用于picker显示）
      const monthLabels = months.map(m => `${m}个月`);

      this.setData({
        years,
        months,
        yearLabels,
        monthLabels
      });

      // 如果有初始值，解析并设置
      if (this.properties.value) {
        this.parseAge(this.properties.value);
      } else {
        // 默认设置为1岁0个月
        this.setData({ yearIndex: 1, monthIndex: 0 });
      }
    }
  },

  /**
   * 组件的方法列表
   */
  methods: {
    /**
     * 年份选择器变更事件
     */
    onYearChange(e) {
      const newYearIndex = parseInt(e.detail.value);
      this.setData({ yearIndex: newYearIndex });
      this.triggerAgeChange();
    },

    /**
     * 月份选择器变更事件
     */
    onMonthChange(e) {
      this.setData({ monthIndex: parseInt(e.detail.value) });
      this.triggerAgeChange();
    },

    /**
     * 触发年龄变更事件
     */
    triggerAgeChange() {
      const age = this.formatAge();
      this.triggerEvent('change', { value: age });
    },

    /**
     * 格式化年龄为"Y/M"格式
     */
    formatAge() {
      const years = this.data.years[this.data.yearIndex];
      const months = this.data.months[this.data.monthIndex];

      // 总月数 = years * 12 + months
      const totalMonths = years * 12 + months;

      // 计算显示的年和月
      const displayYears = Math.floor(totalMonths / 12);
      const displayMonths = totalMonths % 12;

      if (displayYears > 0 && displayMonths > 0) {
        // X岁Y个月：X年Y月
        return `${displayYears}Y${displayMonths}M`;
      } else if (displayYears > 0) {
        // X岁0个月：X年
        return `${displayYears}Y`;
      } else {
        // 0岁Y个月：Y月
        return `${displayMonths}M`;
      }
    },

    /**
     * 解析年龄字符串并设置选择器值
     * @param {String} ageStr - 年龄字符串，如"3Y7M"、"5M"、"3Y"
     */
    parseAge(ageStr) {
      const regex = /^(\d+)Y(\d+)M$|^(\d+)Y$|^(\d+)M$/;
      const match = ageStr.match(regex);

      if (match) {
        if (match[1] !== undefined && match[2] !== undefined) {
          // "3Y7M" 格式 - 有年份有月份
          const years = parseInt(match[1]);
          const months = parseInt(match[2]);
          this.setData({
            yearIndex: years,
            monthIndex: months
          });
        } else if (match[3] !== undefined) {
          // "3Y" 格式 - 只有年份
          const years = parseInt(match[3]);
          this.setData({
            yearIndex: years,
            monthIndex: 0
          });
        } else if (match[4] !== undefined) {
          // "7M" 格式 - 只有月份（未满1岁）
          const months = parseInt(match[4]);
          this.setData({
            yearIndex: 0,
            monthIndex: months
          });
        }
      } else {
        // 无法解析，使用默认值1岁
        this.setData({
          yearIndex: 1,
          monthIndex: 0
        });
      }
    }
  }
});
