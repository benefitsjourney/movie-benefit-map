<script setup>
import { onMounted, ref } from 'vue'
import Swal from 'sweetalert2'
import http from '@/plugins/axios'
import { countyLabel } from '@/constants/county'

const cinemas = ref([])
const loading = ref(true)

function fetchCinemas({ page = 1, size = 200 } = {}) {
  return http.get('/cinemas', { params: { page, size } }).then((response) => response.data)
}

async function fetchAllCinemas() {
  const firstPage = await fetchCinemas({ page: 1, size: 200 })
  const all = [...firstPage.content]

  for (let page = 2; page <= firstPage.totalPages; page += 1) {
    const nextPage = await fetchCinemas({ page, size: firstPage.size })
    all.push(...nextPage.content)
  }

  return all
}

onMounted(async () => {
  try {
    cinemas.value = await fetchAllCinemas()
  } catch (error) {
    loading.value = false
    await Swal.fire({
      icon: 'error',
      title: '無法取得影城資料',
      text: error.message,
    })
    return
  }
  loading.value = false
})
</script>

<template>
  <h1 class="h3 mb-4">影城</h1>

  <div v-if="loading" class="d-flex justify-content-center py-5">
    <div class="spinner-border" role="status">
      <span class="visually-hidden">載入中</span>
    </div>
  </div>

  <p v-else-if="cinemas.length === 0" class="text-body-secondary">目前沒有影城資料</p>

  <div v-else class="row row-cols-1 row-cols-md-2 row-cols-xl-3 g-3">
    <div v-for="cinema in cinemas" :key="cinema.cinemaId" class="col">
      <div class="card h-100">
        <div class="card-body">
          <h2 class="card-title h5">{{ cinema.cinemaName }}</h2>
          <p class="card-text mb-1">{{ countyLabel(cinema.county) }}</p>
          <p class="card-text mb-0">{{ cinema.cinemaAddress || '地址未提供' }}</p>
        </div>
      </div>
    </div>
  </div>
</template>
