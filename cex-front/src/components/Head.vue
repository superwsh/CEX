<template>
  <div>
    <div class="header">
      <div class="container-fluid flex container-head">
        <div class="logo">
          <router-link to="/">
            <b>C2E-Cex</b>
          </router-link>
        </div> 
        <div class="header-item-center warning-text">
          {{ $t('just_for_learning') }}
        </div>

        <div class="regi-cn flex flex-ac">
            <el-menu class="el-menu-nav" mode="horizontal">
              <el-menu-item index="5" @click="dialogVisible = true">{{ diaList[current].name }} </el-menu-item>
            </el-menu>
            <div class="menu-lang">
              <el-button v-if="theme" @click="themeClick('black')"><span class="iconfont icon-sun"></span> </el-button>
              <el-button v-else @click="themeClick('white')"><span class="iconfont icon-moon"></span> </el-button>
            </div>

        <div class="login_out-btn">
          <div @click="logInOutClick">
            {{isLogin ? $t('meun13_5') : $t('meun8')}}
          </div>
          <div @click="registerClick">
            {{ $t('meun9') }}
          </div>
        </div>
        </div>
      </div>
    </div>
    <div class="language">
      <el-dialog :visible.sync="dialogVisible" width="840px" top="18vh" :close-on-click-modal="false">
        <el-tabs v-model="activeName" @tab-click="handleClick">
          <el-tab-pane :label="$t('meun14_1')" name="first">
            <div class="dialog_name">{{ $t('meun14_3') }}</div>
            <div class="dialog_list">
              <el-row>
                <el-col :lg="6" :xs="8" v-for="(item, index) in diaList" :key="index">
                  <div class="dia_col" :class="{ active: index == current }" @click="toggle(item, index)">
                    {{ item.name }}
                  </div>
                </el-col>
              </el-row>
            </div>
          </el-tab-pane>
        </el-tabs>
      </el-dialog>
    </div>
  </div>
</template>

<script>
import { loginOut } from '@/api/api/user'
export default {
  name: 'Header',
  inject: ['reload'],
  data() {
    return {
      activeIndex: '1',
      navinput: '',
      drawer: false,
      direction: 'ltr',
      theme: false,
      dialogVisible: false,
      activeName: 'first',
      diaList: [
        {
          name: 'English',
          value: 'en_US',
        },
        {
          name: '简体中文',
          value: 'zh_CN',
        },
        {
          name: '繁體中文',
          value: 'zh_HK',
        },
        {
          name: '日本語',
          value: 'ja_JP',
        },
        {
          name: '한국인',
          value: 'ko_KR',
        },
        {
          name: 'Deutsch',
          value: 'de_DE',
        },
        {
          name: 'Français',
          value: 'fr_FR',
        },
        {
          name: 'Italiano',
          value: 'it_IT',
        },
        {
          name: 'español',
          value: 'es_ES',
        },
      ],
      currentList: [],
      current: 0,
      current2: 0,
      avatar: require('../assets/photo/user.png'),
      symbol: '',
    }
  },
  created() {
    this.symbol = localStorage.getItem('setSymbol')
  },
  computed: {
    isLogin: function() {
      return this.$store.getters.isLogin
    },
    currentInfo: function() {
      return this.$store.getters.current
    },
  },
  mounted() {
    let theme = localStorage.getItem('theme')
    if (theme == 'black') {
      this.theme = false
    } else {
      this.theme = true
    }
    let isZh = localStorage.getItem('lang')
    if (isZh == 'en_US') {
      this.current = 0
    }
    if (isZh == 'zh_CN') {
      this.current = 1
    }
    if (isZh == 'zh_HK') {
      this.current = 2
    }
    if (isZh == 'ja_JP') {
      this.current = 3
    }
    if (isZh == 'ko_KR') {
      this.current = 4
    }
    if (isZh == 'de_DE') {
      this.current = 5
    }
    if (isZh == 'fr_FR') {
      this.current = 6
    }
    if (isZh == 'it_IT') {
      this.current = 7
    }
    if (isZh == 'es_ES') {
      this.current = 8
    }
  },
  methods: {
    handleSelect(key, keyPath) {
      console.log(key, keyPath)
    },
    logInOutClick() {
      if (this.isLogin) {
        this.logOutClick()
      } else {
        this.$router.push({
          path: '/login',
        })
      }
    },
    registerClick() {
      this.$router.push({
        path: '/register',
      })
    },
    logOutClick() {
      loginOut().then(res => {
        if (res.code == 0) {
          localStorage.removeItem('token')
          this.$store.commit('SET_ISLOGIN', false)
          this.$store.commit('SET_MEMBER', null)
          this.isLogin = false
          this.$message({
            message: 'Login out successfully',
            type: 'success',
          })
          this.$router.push({
            path: '/',
          })
        }
      })
    },
    themeClick(theme) {
      this.theme = !this.theme
      this.$store.dispatch('setTheme', theme)
      window.document.documentElement.setAttribute('data-theme', theme)
    },
    handleClick(tab, event) {
      console.log(tab, event)
    },
    toggle(item, index) {
      this.current = index
      this.dialogVisible = false
      this.$i18n.locale = item.value
      this.$emit('newLocal', item.value)
      localStorage.setItem('lang', item.value)
      this.reload()
    },
    langClick() {
      this.activeName = 'first'
      this.dialogVisible = true
    },
  },
  watch: {},
}
</script>
<style>
@media (max-width: 1300px) {
  .hidden-sm {
    display: none !important;
  }
}

@media (max-width: 1200px) {
  .hidden-xs {
    display: none !important;
  }

  .el-menu-item,
  .el-submenu__title {
    height: 42px !important;
    line-height: 42px !important;
  }

  .el-drawer__header {
    margin-bottom: 0 !important;
  }

  .menu-nav {
    width: 100% !important;
    padding: 0 20px !important;
  }

  .el-menu-user .el-submenu__title {
    padding: 0 5px;
  }
  .head-body .el-menu--horizontal > .el-submenu .el-submenu__icon-arrow {
    margin-left: 8px;
    margin-top: 0;
  }

  .el-drawer__body {
    overflow-y: auto;
  }

  .el-menu-btns.el-menu-nav > .el-menu-item {
    padding: 0 15px !important;
  }

  .el-menu-btns.el-menu-nav > .el-menu-item:first-child {
    padding-left: 0 !important;
  }
}

@media (max-width: 768px) {
  .el-menu-btns.el-menu-nav > .el-menu-item {
    padding: 0 !important;
    max-width: 50px;
    margin-right: 7px !important;
  }

  .menu-nav .unit-span {
    margin: 0;
    padding: 0 10px;
  }

  .menu-nav {
    padding: 0 0 0 20px !important;
  }
}

.custom-drawer .el-drawer__close-btn {
  padding: 0;
}
.logo {
  margin-right: 60px;
  display: flex;
  justify-content: center;
  align-items: center;
}
.logo a {
  display: flex;
  justify-content: center;
  align-items: center;
}
.logo img {
  width: 22px;
}
.logo b {
  color: #689AFF;
  padding-left: 6px;
  font-size: 20px;
  letter-spacing: 1px;
}
.login_out-btn {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 150px;
  color: #689AFF;
  cursor: pointer;
  border-radius: 5px;
  gap: 10px;
}
.container-head {
  justify-content: space-between;
}
.header-item-center {
  display: flex;
  justify-content: center;
  align-items: center;
}
.warning-text {
  color: #FF0000;
}
</style>