import request from '@/utils/request'

// 提交分析任务（POST /tasks，字段对应 MrJobLog 实体：jobName/jobType/inputPath/outputPath）
export function submitTask(data) {
  return request({
    url: '/tasks',
    method: 'post',
    data
  })
}

// 查询任务列表（pageNum/pageSize/status/keyword）
export function getTaskList(params) {
  return request({
    url: '/tasks',
    method: 'get',
    params
  })
}

// 查询任务详情
export function getTaskDetail(id) {
  return request({
    url: `/tasks/${id}`,
    method: 'get'
  })
}

// 重试任务
export function retryTask(id) {
  return request({
    url: `/tasks/${id}/retry`,
    method: 'post'
  })
}

// 删除任务
export function deleteTask(id) {
  return request({
    url: `/tasks/${id}`,
    method: 'delete'
  })
}
