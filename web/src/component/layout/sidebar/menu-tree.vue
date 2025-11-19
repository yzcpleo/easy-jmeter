<template>
  <!--el-submenu改名为el-sub-menu-->
  <el-sub-menu v-if="item && item.children && item.children.length > 0" :index="item.path || ''">
    <template #title>
      <i v-if="item.icon && !filterIcon(item.icon)" :class="item.icon"></i>
      <img v-else-if="item.icon && filterIcon(item.icon)" :src="item.icon" class="img-icon" />
      <span>{{ item.title || '' }}</span>
    </template>
    <menu-tree 
      v-for="(child, index) in validChildren" 
      :key="child.path || `menu-item-${index}`" 
      :item="child" 
    />
  </el-sub-menu>

  <el-menu-item v-else-if="item && item.path" :index="item.path" @click="navigateTo(item.path)">
    <i v-if="item.icon && !filterIcon(item.icon)" :class="item.icon"></i>
    <img v-else-if="item.icon && filterIcon(item.icon)" :src="item.icon" class="img-icon" />
    <template #title
      ><span class="title">{{ item.title || '' }}</span></template
    >
  </el-menu-item>
</template>

<script>
export default {
  name: 'MenuTree',
  props: {
    item: {
      type: Object,
      required: true,
    },
  },

  computed: {
    validChildren() {
      // 过滤掉 null 和 undefined 的子项
      if (!this.item || !this.item.children) {
        return []
      }
      return this.item.children.filter(child => child != null)
    },
  },

  methods: {
    navigateTo(path) {
      if (path && this.$router) {
        this.$router.push({ path }).catch(err => {
          console.warn('Navigation error:', err)
        })
      }
    },
    filterIcon(icon) {
      return icon && typeof icon === 'string' && icon.indexOf('/') !== -1
    },
  },
}
</script>

<style lang="scss" scoped>
.img-icon {
  width: 16px;
  height: 16px;
  margin-right: 10px;
  margin-left: 5px;
  display: inline-block;
  transform: translateY(21px);
}

.iconfont {
  margin-right: 10px;
  margin-left: 5px;
  color: $sub-menu-title;
  height: $menu-item-height;
}

.title {
  display: inline-block;
  width: 110px;
  @include no-wrap();
}
</style>
