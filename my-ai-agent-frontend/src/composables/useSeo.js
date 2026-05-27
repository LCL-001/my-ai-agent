import { onMounted } from 'vue'

function setMeta(name, content) {
  if (!content) return

  let meta = document.querySelector(`meta[name="${name}"]`)
  if (!meta) {
    meta = document.createElement('meta')
    meta.setAttribute('name', name)
    document.head.appendChild(meta)
  }
  meta.setAttribute('content', content)
}

function setPropertyMeta(property, content) {
  if (!content) return

  let meta = document.querySelector(`meta[property="${property}"]`)
  if (!meta) {
    meta = document.createElement('meta')
    meta.setAttribute('property', property)
    document.head.appendChild(meta)
  }
  meta.setAttribute('content', content)
}

export function useSeo({ title, description, keywords }) {
  onMounted(() => {
    document.title = title
    setMeta('description', description)
    setMeta('keywords', keywords)
    setPropertyMeta('og:title', title)
    setPropertyMeta('og:description', description)
    setPropertyMeta('og:type', 'website')
  })
}
