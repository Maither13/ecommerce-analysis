import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('user_token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('user_info') || 'null'))

  function setLogin(data) {
    token.value = data.token
    userInfo.value = data.userInfo
    localStorage.setItem('user_token', data.token)
    localStorage.setItem('user_info', JSON.stringify(data.userInfo))
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('user_token')
    localStorage.removeItem('user_info')
  }

  const isLoggedIn = () => !!token.value

  return { token, userInfo, setLogin, logout, isLoggedIn }
})
