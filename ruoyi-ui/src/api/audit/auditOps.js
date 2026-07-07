import request from '@/utils/request'

export const ops = {
  schemeList: (pId) => request({url:'/audit/ops/scheme/list',params:{projectId:pId}}),
  addScheme: (d) => request({url:'/audit/ops/scheme',method:'post',data:d}),
  wpList: (q) => request({url:'/audit/ops/wp/list',params:q}),
  wpInfo: (id) => request({url:'/audit/ops/wp/'+id}),
  addWp: (d) => request({url:'/audit/ops/wp',method:'post',data:d}),
  editWp: (d) => request({url:'/audit/ops/wp',method:'put',data:d}),
  reviewList: (wId) => request({url:'/audit/ops/review/list',params:{workpaperId:wId}}),
  addReview: (d) => request({url:'/audit/ops/review',method:'post',data:d}),
  reportList: (pId) => request({url:'/audit/ops/report/list',params:{projectId:pId}}),
  reportInfo: (id) => request({url:'/audit/ops/report/'+id}),
  addReport: (d) => request({url:'/audit/ops/report',method:'post',data:d}),
  editReport: (d) => request({url:'/audit/ops/report',method:'put',data:d}),
  collabLog: (pId) => request({url:'/audit/ops/collab/list',params:{projectId:pId}}),
}
