import { defineStore } from 'pinia'

const DRAFT_KEY = 'travel-assistant-draft'
const REQUEST_KEY = 'travel-assistant-request'
const MESSAGE_KEY = 'travel-assistant-messages'
const ITINERARY_KEY = 'travel-assistant-itinerary-id'
const MODIFIED_KEY = 'travel-assistant-is-modified'

export const usePlannerStore = defineStore('planner', {
  state: () => ({
    currentDraft: JSON.parse(sessionStorage.getItem(DRAFT_KEY) || 'null'),
    draft: JSON.parse(sessionStorage.getItem(DRAFT_KEY) || 'null'),
    requestContext: JSON.parse(sessionStorage.getItem(REQUEST_KEY) || 'null'),
    messages: JSON.parse(sessionStorage.getItem(MESSAGE_KEY) || '[]'),
    itineraryId: JSON.parse(sessionStorage.getItem(ITINERARY_KEY) || 'null'),
    isModified: JSON.parse(sessionStorage.getItem(MODIFIED_KEY) || 'false'),
  }),
  actions: {
    persistState() {
      sessionStorage.setItem(ITINERARY_KEY, JSON.stringify(this.itineraryId))
      sessionStorage.setItem(DRAFT_KEY, JSON.stringify(this.currentDraft))
      sessionStorage.setItem(REQUEST_KEY, JSON.stringify(this.requestContext))
      sessionStorage.setItem(MESSAGE_KEY, JSON.stringify(this.messages))
      sessionStorage.setItem(MODIFIED_KEY, JSON.stringify(this.isModified))
    },
    setSession(session, requestContext) {
      this.itineraryId = session?.itineraryId ?? null
      this.currentDraft = session?.currentDraft ?? null
      this.draft = session?.currentDraft ?? null
      this.requestContext = requestContext
      this.messages = session?.messages ?? []
      this.isModified = false
      this.persistState()
    },
    setDraft(draft, requestContext, messages = this.messages) {
      this.currentDraft = draft
      this.draft = draft
      this.requestContext = requestContext
      this.messages = messages
      this.persistState()
    },
    setMessages(messages) {
      this.messages = messages
      this.persistState()
    },
    persistDraft() {
      this.draft = this.currentDraft
      this.persistState()
    },
    markModified() {
      this.isModified = true
      this.persistDraft()
    },
    resetModified() {
      this.isModified = false
      this.persistState()
    },
    clearDraft() {
      this.currentDraft = null
      this.draft = null
      this.requestContext = null
      this.messages = []
      this.itineraryId = null
      this.isModified = false
      sessionStorage.removeItem(DRAFT_KEY)
      sessionStorage.removeItem(REQUEST_KEY)
      sessionStorage.removeItem(MESSAGE_KEY)
      sessionStorage.removeItem(ITINERARY_KEY)
      sessionStorage.removeItem(MODIFIED_KEY)
    },
  },
})
