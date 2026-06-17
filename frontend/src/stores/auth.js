import { defineStore } from 'pinia'

const STORAGE_KEY = 'travel-assistant-user'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null'),
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.user?.id),
    isAdmin: (state) => state.user?.role === 'ADMIN',
  },
  actions: {
    setUser(user) {
      this.user = user
      localStorage.setItem(STORAGE_KEY, JSON.stringify(user))
    },
    logout() {
      this.user = null
      localStorage.removeItem(STORAGE_KEY)
    },
  },
})
