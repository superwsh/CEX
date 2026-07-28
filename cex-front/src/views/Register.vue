<template>
  <div class="page-web page_regi">
    <Head :MyLocal="location" @newLocal="location = $event" />
    <div class="regi-bd">
      <div class="regi_name">{{ $t('account3') }}</div>
      <el-form ref="form2" :rules="rules2" class="regi-from" :model="form2">
        <el-form-item prop="phone">
          <div class="regi_group">
            <div class="regi_gr_t">{{ $t('phone') }}</div>
            <div class="regi_gr_b">
              <el-input v-model="form2.phone" placeholder=""></el-input>
            </div>
          </div>
        </el-form-item>
        <el-form-item prop="password2">
          <div class="regi_group">
            <div class="regi_gr_t">{{ $t('password') }}</div>
            <div class="regi_gr_b">
              <el-input v-model="form2.password2" placeholder="" :type="pass2 ? 'password' : 'text'"> </el-input>
              <div class="regi_eye" @click="eye2">
                <i class="iconfont icon-eye-close" v-if="Eye2"></i>
                <i class="iconfont icon-eye" v-else></i>
              </div>
            </div>
          </div>
        </el-form-item>
        <el-form-item prop="checkpassword2">
          <div class="regi_group">
            <div class="regi_gr_t">{{ $t('password4') }}</div>
            <div class="regi_gr_b">
              <el-input v-model="form2.checkpassword2" placeholder="" :type="pass3 ? 'password' : 'text'"> </el-input>
              <div class="regi_eye" @click="eye3">
                <i class="iconfont icon-eye-close" v-if="Eye3"></i>
                <i class="iconfont icon-eye" v-else></i>
              </div>
            </div>
          </div>
        </el-form-item>
        <el-button class="btn" :plain="true" @click="submitForm('form2')">{{ $t('register') }} </el-button>
      </el-form>
    </div>
  </div>
</template>
<script>
import Head from '@/components/Head'
import { getCountry, checkUsername, regPhone } from '@/api/api/user'
export default {
  components: {
    Head,
  },
  data() {
    const validateUser = (rule, value, callback) => {
      var reg = /^[a-z0-9]+([._\\-]*[a-z0-9])*@([a-z0-9]+[-a-z0-9]*[a-z0-9]+.){1,63}[a-z0-9]+$/
      // reg = /^(\w)+(\.\w+)*@(\w)+((\.\w{2,3}){1,3})$/
      // if (value == '') {
      //   callback(new Error(this.$t('mailtip')))
      // } else if (!reg.test(this.form2.email)) {
      //   callback(new Error(this.$t('emailerr2')))
      // } else {
        // callback()
      // }
    }
    var validatePass = (rule, value, callback) => {
      if (value === '') {
        callback(new Error(this.$t('logErr2')))
      } else {
        if (this.form.checkpassword !== '') {
          this.$refs.form.validateField('checkpassword')
        }
        callback()
      }
    }
    var validatePass2 = (rule, value, callback) => {
      if (value === '') {
        callback(new Error(this.$t('confirmpwdtip')))
      } else if (value !== this.form.password) {
        callback(new Error(this.$t('confirmpwderr')))
      } else {
        callback()
      }
    }
    var validatePass3 = (rule, value, callback) => {
      if (value === '') {
        callback(new Error(this.$t('logErr2')))
      } else {
        if (this.form2.checkpassword2 !== '') {
          this.$refs.form2.validateField('checkpassword2')
        }
        callback()
      }
    }
    var validatePass4 = (rule, value, callback) => {
      if (value === '') {
        callback(new Error(this.$t('confirmpwdtip')))
      } else if (value !== this.form2.password2) {
        callback(new Error(this.$t('confirmpwderr')))
      } else {
        callback()
      }
    }
    return {
      show: false,
      activeName: 'second',
      Eye: true,
      Eye1: true,
      Eye2: true,
      Eye3: true,
      pass: true,
      pass1: true,
      pass2: true,
      pass3: true,
      country: [],
      countryImageUrl: '',
      form: {
        country: '中国',
        phone: '',
        code: '',
        password: '',
        checkpassword: '',
        invite: '',
        check: [],
      },
      rules: {
        phone: [
          {
            required: true,
            message: this.$t('logErr'),
            trigger: 'blur',
          },
        ],
        code: [
          {
            required: true,
            message: this.$t('regErr'),
          },
        ],
        password: [
          {
            validator: validatePass,
            trigger: 'blur',
          },
          {
            type: 'string',
            min: 6,
            message: this.$t('logErr3'),
            trigger: 'blur',
          },
        ],
        checkpassword: [
          {
            validator: validatePass2,
            trigger: 'blur',
          },
        ],
        check: [
          {
            type: 'array',
            required: true,
            message: this.$t('agreementtip'),
            trigger: 'change',
          },
        ],
      },
      form2: {
        phone: '',
        code2: '',
        password2: '',
        checkpassword2: '',
        invite2: '',
        check: [],
      },
      rules2: {
        email: [
          {
            required: true,
            validator: validateUser,
            trigger: 'blur',
          },
        ],
        code2: [
          {
            required: true,
            message: this.$t('regErr'),
          },
        ],
        password2: [
          {
            validator: validatePass3,
            trigger: 'blur',
          },
          {
            type: 'string',
            min: 6,
            message: this.$t('logErr3'),
            trigger: 'blur',
          },
        ],
        checkpassword2: [
          {
            validator: validatePass4,
            trigger: 'blur',
          },
        ],
        check: [
          {
            type: 'array',
            required: true,
            message: this.$t('agreementtip'),
            trigger: 'change',
          },
        ],
      },
      location: 'en_US',
      dialogVisible: false,
      waitTime: 60,
      count: '60s',
      disabled: true,
      waitTime2: 60,
      count2: '60s',
      disabled2: true,
      screenWidth: null,
    }
  },
  created() {
    this.location = localStorage.getItem('lang')
    let invite = this.$route.query.code
    if (invite != undefined) {
      this.form.invite = invite
      this.form2.invite2 = invite
    }
  },
  watch: {},
  mounted() {
    this.countryList()
  },
  methods: {
    countryList() {
      getCountry().then(res => {
        if (res.code == 0) {
          console.log(res)
          this.country = res.data
          this.countryImageUrl = this.country[0].countryImageUrl
        }
      })
    },
    countryItem(url) {
      this.countryImageUrl = url
    },
    sendEmail() {
      var reg = /^[a-z0-9]+([._\\-]*[a-z0-9])*@([a-z0-9]+[-a-z0-9]*[a-z0-9]+.){1,63}[a-z0-9]+$/
      reg = /^(\w)+(\.\w+)*@(\w)+((\.\w{2,3}){1,3})$/
      if (this.form2.email == '') {
        this.$message.error(this.$t('mailtip'))
      } else {
        this.dialogVisible = true
      }
    },
    sendMobile() {
      if (this.form.phone == '') {
        this.$message.error(this.$t('chtip9'))
      } else {
        this.dialogVisible = true
      }
    },
    submitForm(formName) {
      this.$refs[formName].validate(valid => {
        if (valid) {
          checkUsername({
            username: this.form2.phone,
          }).then(res => {
            if (res.code == 0) {
              regPhone({
                code: 123456,
                country: '中国',
                password: this.form2.password2,
                phone: this.form2.phone,
                promotion: this.form.invite,
                randStr: '',
                superPartner: '',
                ticket: '',
                username: this.form2.phone,
                validate: '',
              }).then(res => {
                if (res.code == 0) {
                  this.$message({
                    message: res.message,
                    type: 'success',
                  })
                  this.$refs[formName].resetFields()
                  this.$router.push({
                    path: '/login',
                  })
                } else {
                  this.$message({
                    message: res.message,
                    type: 'error',
                  })
                }
              })
            }else if(res.code === 500){
              this.$message({
                message: res.message,
                type: 'error',
              })
            }
          })

          console.log('submit!!')
        } else {
          console.log('error submit!!')
          console.log(this.form)
          return false
        }
      })
    },
    handleClick(tab, event) {
      console.log(tab, event)
    },
    eye() {
      this.Eye = !this.Eye
      this.pass = !this.pass
    },
    eye1() {
      this.Eye1 = !this.Eye1
      this.pass1 = !this.pass1
    },
    eye2() {
      this.Eye2 = !this.Eye2
      this.pass2 = !this.pass2
    },
    eye3() {
      this.Eye3 = !this.Eye3
      this.pass3 = !this.pass3
    },
  },
}
</script>
