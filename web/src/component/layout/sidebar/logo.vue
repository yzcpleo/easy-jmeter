<template>
  <div :class="!elMenuCollapse ? 'logo' : 'mobile-logo'">
    <span class="logo-text">{{ !elMenuCollapse ? '国际业务性能测试平台' : '测试平台' }}</span>
  </div>
</template>

<script>
export default {
  props: {
    elMenuCollapse: {
      type: Boolean,
      required: true,
    },
  },
}
</script>

<style lang="scss" scoped>
.logo {
  width: $sidebar-width;
  height: $header-height;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #122150;
  transition: all 0.3s linear;
  position: sticky;
  top: 0;
  left: 0;
  z-index: 99;

  .logo-text {
    font-size: 18px;
    font-weight: bold;
    color: #fff;
    text-align: center;
    line-height: 1.2;
  }
}

.mobile-logo {
  width: 64px;
  height: 86px;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #122150;
  transition: all 0.3s linear;

  .logo-text {
    font-size: 12px;
    font-weight: bold;
    color: #fff;
    text-align: center;
    line-height: 1.2;
    writing-mode: vertical-rl;
    text-orientation: mixed;
  }
}
</style>
