package xyz.anythings.gw.service.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import xyz.anythings.base.LogisConstants;
import xyz.anythings.base.entity.Gateway;
import xyz.anythings.base.entity.Indicator;
import xyz.anythings.base.entity.JobBatch;
import xyz.anythings.base.entity.JobInstance;
import xyz.anythings.base.entity.WorkCell;
import xyz.anythings.gw.MwConstants;
import xyz.anythings.gw.model.GatewayInitResIndList;
import xyz.anythings.gw.model.IndicatorOnInformation;
import xyz.anythings.gw.service.IndSendService;
import xyz.anythings.gw.service.model.IndCommonReq;
import xyz.anythings.gw.service.model.IndOnPickReq;
import xyz.anythings.sys.util.AnyOrmUtil;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.sys.SysConstants;
import xyz.elidom.sys.util.DateUtil;
import xyz.elidom.sys.util.ValueUtil;
import xyz.elidom.util.BeanUtil;

/**
 * 표시기 서비스 유틸리티
 * 
 * @author shortstop
 */
public class IndServiceUtil {
	
	/**
	 * 호기내 로케이션들 중에 거래처 매핑된 
	 * 
	 * @param domainId
	 * @param jobType
	 * @param indList
	 */
	public static int indOnNoboxDisplay(Long domainId, String jobType, List<IndCommonReq> indList) {
		// 1. 빈 값 체크 
		if(ValueUtil.isNotEmpty(indList)) {
			IndSendService sendSvc = BeanUtil.get(IndSendService.class);
			
			// 2. 점등 요청을 위한 데이터 모델 생성. 
			Map<String, List<IndicatorOnInformation>> indOnInfoList = new HashMap<String, List<IndicatorOnInformation>>();

			for (IndCommonReq indOnPick : indList) {
				String gwPath = indOnPick.getGwPath();
				List<IndicatorOnInformation> indOnList = 
						indOnInfoList.containsKey(gwPath) ? indOnInfoList.get(gwPath) : new ArrayList<IndicatorOnInformation>();
				IndicatorOnInformation indOnInfo = new IndicatorOnInformation();
				indOnInfo.setId(indOnPick.getIndCd());
				indOnInfo.setBizId(indOnPick.getIndCd());
				indOnList.add(indOnInfo);
				indOnInfoList.put(gwPath, indOnList);
			}
					
			if(ValueUtil.isNotEmpty(indOnInfoList)) {
				// 3. 표시기 점등 요청
				sendSvc.requestIndsOn(domainId, jobType, MwConstants.IND_ACTION_TYPE_NOBOX, indOnInfoList);
				// 4. 점등된 표시기 개수 리턴 
				return indOnInfoList.size();
			}
		}
		
		return 0;		
	}
	
	/**
	 * 분류 작업 완료된 작업 리스트의 처리 수량을 표시기에 표시 
	 * 
	 * @param jobType
	 * @param jobList
	 */
	public static void restoreIndDisplayJobPicked(String jobType, List<JobInstance> jobList) {
		
		if(ValueUtil.isNotEmpty(jobList)) {
			IndSendService sendSvc = BeanUtil.get(IndSendService.class);
			
			// 점등 요청을 위한 데이터 모델 생성. 
			Map<String, List<IndicatorOnInformation>> indOnInfoList = new HashMap<String, List<IndicatorOnInformation>>();
			Long domainId = null;
			
			for (JobInstance job : jobList) {
				if(domainId == null) {
					domainId = job.getDomainId();
				}
				
				String gwPath = job.getGwPath();
				List<IndicatorOnInformation> indOnList = 
						indOnInfoList.containsKey(gwPath) ? indOnInfoList.get(gwPath) : new ArrayList<IndicatorOnInformation>();
				IndicatorOnInformation indOnInfo = new IndicatorOnInformation();
				indOnInfo.setId(job.getIndCd());
				indOnInfo.setBizId(job.getId());
				indOnInfo.setOrgEaQty(job.getPickedQty());
				indOnList.add(indOnInfo);
				indOnInfoList.put(gwPath, indOnList);
			}
			
			// 표시기 점등 요청
			if(ValueUtil.isNotEmpty(indOnInfoList)) {
				sendSvc.requestIndsOn(domainId, jobType, MwConstants.IND_ACTION_TYPE_DISPLAY, indOnInfoList);
			}
		}
	}
	
	/**
	 * 작업이 완료된 표시기에 END 표시를 복원
	 * 
	 * @param batch
	 * @param gateway
	 * @return
	 */
	public static List<WorkCell> restoreIndDisplayBoxingEnd(JobBatch batch, Gateway gateway) {
		// 1. DAS, RTN에 대해서 로케이션의 jobStatus가 END, ENDED 상태인 모든 로케이션을 조회
		Query condition = AnyOrmUtil.newConditionForExecution(batch.getDomainId(), 0, 0, "domain_id", "cell_cd", "ind_cd", "status", "job_instance_id");
		condition.addFilter("indCd", SysConstants.IN, gateway.indCdList());
		condition.addFilter("status", SysConstants.IN, LogisConstants.CELL_JOB_STATUS_END_LIST);
		condition.addOrder("cellCd", true);
		List<WorkCell> workCells = BeanUtil.get(IQueryManager.class).selectList(WorkCell.class, condition);
		
		// 2. 로케이션 별로 상태별로 END (ReadOnly = false), END (ReadOnly = true)를 표시
		return restoreIndDisplayBoxingEnd(batch.getJobType(), workCells);
	}
	
	/**
	 * 호기, 작업 존에 작업이 완료된 표시기에 END 표시를 복원
	 * 
	 * @param domainId
	 * @param jobType
	 * @param rackCd
	 * @param equipZoneCd
	 * @return
	 */
	public static List<WorkCell> restoreIndDisplayBoxingEnd(Long domainId, String jobType, String rackCd, String equipZoneCd) {
		// 1. DAS, RTN에 대해서 로케이션의 jobStatus가 END, ENDED 상태인 모든 로케이션을 조회
		Query condition = AnyOrmUtil.newConditionForExecution(domainId, 0, 0,  "domain_id", "cell_cd", "ind_cd", "status", "job_instance_id");
		condition.addFilter("rackCd", rackCd);
		condition.addFilter("status", SysConstants.IN, LogisConstants.CELL_JOB_STATUS_END_LIST);
		condition.addOrder("cellCd", true);
		List<WorkCell> workCells = BeanUtil.get(IQueryManager.class).selectList(WorkCell.class, condition);
		
		// 2. 로케이션 별로 상태별로 END (ReadOnly = false), END (ReadOnly = true)를 표시
		return restoreIndDisplayBoxingEnd(jobType, workCells);
	}
	
	/**
	 * 로케이션 별로 상태별로 END (ReadOnly = false), END (ReadOnly = true)를 표시
	 * 
	 * @param jobType
	 * @param workCells
	 * @return
	 */
	public static List<WorkCell> restoreIndDisplayBoxingEnd(String jobType, List<WorkCell> workCells) {
		if(ValueUtil.isNotEmpty(workCells)) {
			IndSendService indSendService = BeanUtil.get(IndSendService.class);

			for(WorkCell cell : workCells) {
				String jobStatus = cell.getStatus();
				
				if(ValueUtil.isNotEmpty(jobStatus)) {
					if(ValueUtil.isEqual(LogisConstants.CELL_JOB_STATUS_END, jobStatus)) {
						String bizId = ValueUtil.isEmpty(cell.getJobInstanceId()) ? cell.getIndCd() : cell.getJobInstanceId();
						indSendService.requestIndEndDisplay(cell.getDomainId(), jobType, cell.getIndCd(), bizId, false);
						
					} else if(ValueUtil.isEqual(LogisConstants.CELL_JOB_STATUS_ENDED, jobStatus)) {
						String bizId = ValueUtil.isEmpty(cell.getJobInstanceId()) ? cell.getIndCd() : cell.getJobInstanceId();
						indSendService.requestIndEndDisplay(cell.getDomainId(), jobType, cell.getIndCd(), bizId, true);
					}
				}			
			}
		}
		
		return workCells;
	}
	
	/**
	 * 작업 데이터로 표시기를 점등한다.
	 * 
	 * @param needUpdateJobStatus 
	 * @param job
	 * @return
	 */
	public static boolean indOnByJob(boolean needUpdateJobStatus, JobInstance job) {
		if(ValueUtil.isEmpty(job.getGwPath())) {
			String gwPath = Indicator.findGatewayPath(job.getDomainId(), job.getIndCd());
			job.setGwPath(gwPath);
		}
		
		List<JobInstance> jobList = ValueUtil.toList(job);
		IndServiceUtil.indOnByJobList(needUpdateJobStatus, job.getJobType(), jobList);
		return true;
	}
	
	/**
	 * 작업 데이터로 표시기를 점등한다.
	 * 
	 * @param needUpdateJobStatus 
	 * @param job
	 * @param showPickingQty 작업의 피킹 수량을 표시기의 분류 수량으로 표시할 지 여부
	 * @return
	 */
	public static boolean indOnByJob(boolean needUpdateJobStatus, JobInstance job, boolean showPickingQty) {
		if(ValueUtil.isEmpty(job.getGwPath())) {
			String gwPath = Indicator.findGatewayPath(job.getDomainId(), job.getIndCd());
			job.setGwPath(gwPath);
		}
		
		List<JobInstance> jobList = ValueUtil.toList(job);
		IndServiceUtil.indOnByJobList(needUpdateJobStatus, job.getJobType(), jobList, showPickingQty);
		return true;
	}
	
	/**
	 * 작업 리스트 정보로 표시기 점등 
	 * 
	 * @param needUpdateJobStatus Job 데이터의 상태 변경이 필요한 지 여부
	 * @param jobType DPS, DAS, RTN
	 * @param jobList 작업 데이터 리스트
	 * @return 점등된 표시기 개수 리턴
	 */
	public static int indOnByJobList(boolean needUpdateJobStatus, String jobType, List<JobInstance> jobList) {
		// 1. 빈 값 체크 
		if(ValueUtil.isNotEmpty(jobList)) {
			// 2. 점등 요청을 위한 데이터 모델 생성. 
			Map<String, List<IndicatorOnInformation>> indOnList = buildIndOnList(needUpdateJobStatus, jobType, jobList, false);
			
			if(ValueUtil.isNotEmpty(indOnList)) {
				JobInstance firstJob = jobList.get(0);
				// 3. 표시기 점등 요청
				BeanUtil.get(IndSendService.class).requestIndsOn(firstJob.getDomainId(), jobType, MwConstants.IND_ACTION_TYPE_PICK, indOnList);
				// 4. 점등된 표시기 개수 리턴 
				return indOnList.size();
			}
		}
		
		return 0;
	}
	
	/**
	 * 작업 리스트 정보로 표시기 점등 
	 * 
	 * @param needUpdateJobStatus Job 데이터의 상태 변경이 필요한 지 여부
	 * @param jobType DPS, DAS, RTN
	 * @param jobList 작업 데이터 리스트
	 * @param showPickingQty 작업의 피킹 수량을 표시기의 분류 수량으로 표시할 지 여부
	 * @return 점등된 표시기 개수 리턴
	 */
	public static int indOnByJobList(boolean needUpdateJobStatus, String jobType, List<JobInstance> jobList, boolean showPickingQty) {
		// 1. 빈 값 체크 
		if(ValueUtil.isNotEmpty(jobList)) {
			// 2. 점등 요청을 위한 데이터 모델 생성. 
			Map<String, List<IndicatorOnInformation>> indOnList = buildIndOnList(needUpdateJobStatus, jobType, jobList, true);
			
			if(ValueUtil.isNotEmpty(indOnList)) {
				JobInstance firstJob = jobList.get(0);
				// 3. 표시기 점등 요청
				BeanUtil.get(IndSendService.class).requestIndsOn(firstJob.getDomainId(), jobType, MwConstants.IND_ACTION_TYPE_PICK, indOnList);
				// 4. 점등된 표시기 개수 리턴 
				return indOnList.size();
			}
		}
		
		return 0;
	}
	
	/**
	 * 작업 리스트 정보 중 피킹 상태의 정보만 표시기 점등
	 * 
	 * @param needUpdateJobStatus
	 * @param jobType
	 * @param jobList
	 * @return
	 */
	public static int indOnByPickingJobList(boolean needUpdateJobStatus, boolean qytNoCheck, String jobType, List<JobInstance> jobList) {
		// 1. 빈 값 체크 
		if(ValueUtil.isNotEmpty(jobList)) {
			List<JobInstance> pickingJobs = new ArrayList<JobInstance>(jobList.size());
			
			for(JobInstance job : jobList) {
				// 피킹 예정 수량이 피킹 확정 수량보다 큰 것만 표시기 점등 
				if(qytNoCheck || (job.getPickQty() > job.getPickedQty())) {
					pickingJobs.add(job);
				}
			}
			
			if(ValueUtil.isNotEmpty(pickingJobs)) {
	 			// 2. 점등 요청을 위한 데이터 모델 생성. 
				Map<String, List<IndicatorOnInformation>> indOnList = buildIndOnList(needUpdateJobStatus, jobType, jobList, false);
				
				if(ValueUtil.isNotEmpty(indOnList)) {
					JobInstance firstJob = pickingJobs.get(0);
					// 3. 표시기 점등 요청
					BeanUtil.get(IndSendService.class).requestIndsOn(firstJob.getDomainId(), jobType, MwConstants.IND_ACTION_TYPE_PICK, indOnList);
					// 4. 점등된 표시기 개수 리턴 
					return indOnList.size();
				}
			}
		}
		
		return 0;
	}
	
	/**
	 * 작업 리스트 정보 중 피킹 상태의 정보만 표시기 점등
	 * 
	 * @param needUpdateJobStatus
	 * @param jobType
	 * @param jobList
	 * @return
	 */
	public static int indDisplayByPickingJobList(boolean needUpdateJobStatus, boolean qytNoCheck, String jobType, List<JobInstance> jobList) {
		// 1. 빈 값 체크 
		if(ValueUtil.isNotEmpty(jobList)) {
			List<JobInstance> pickingJobs = new ArrayList<JobInstance>(jobList.size());
			
			for(JobInstance job : jobList) {
				// 피킹 예정 수량이 피킹 확정 수량보다 큰 것만 표시기 점등 
				if(qytNoCheck || (job.getPickQty() > job.getPickedQty())) {
					pickingJobs.add(job);
				}
			}
			
			if(ValueUtil.isNotEmpty(pickingJobs)) {
	 			// 2. 점등 요청을 위한 데이터 모델 생성. 
				Map<String, List<IndicatorOnInformation>> indOnList = buildIndOnList(needUpdateJobStatus, jobType, jobList, false);
				
				if(ValueUtil.isNotEmpty(indOnList)) {
					JobInstance firstJob = pickingJobs.get(0);
					// 3. 표시기 점등 요청
					BeanUtil.get(IndSendService.class).requestIndsOn(firstJob.getDomainId(), jobType, MwConstants.IND_ACTION_TYPE_DISPLAY, indOnList);
					// 4. 점등된 표시기 개수 리턴 
					return indOnList.size();
				}
			}
		}
		
		return 0;
	}
	
	/**
	 * 작업 리스트 정보로 표시기 점등 
	 * 
	 * @param jobType DPS, DAS, RTN
	 * @param jobList 작업 데이터 리스트
	 * @return 점등된 표시기 개수 리턴
	 */
	public static int indOnByInspectJobList(String jobType, List<JobInstance> jobList) {
		// 1. 빈 값 체크 
		if(ValueUtil.isNotEmpty(jobList)) {
			
			// 2. 검수 색깔은 빨간색으로 고정
			for(JobInstance job : jobList) {
				job.setColorCd(MwConstants.COLOR_RED);
			}
			
			// 3. 점등 요청을 위한 데이터 모델 생성. 
			Map<String, List<IndicatorOnInformation>> indOnList = buildIndOnList(false, jobType, jobList, false);
			
			if(ValueUtil.isNotEmpty(indOnList)) {
				// 4. 표시기 점등 요청
				BeanUtil.get(IndSendService.class).requestIndsInspectOn(jobList.get(0).getDomainId(), jobType, indOnList);
				// 5. 점등된 표시기 개수 리턴 
				return indOnList.size();
			}
		}
		
		return 0;
	}
	
	/**
	 * jobList로 부터 표시기 점등 모델을 생성하여 리턴 
	 * 
	 * @param needUpdateJobStatus 표시기 점등 후 Job 데이터의 상태 변경이 필요한 지 여부
	 * @param jobType
	 * @param jobList
	 * @param showPickingQty JobProcess의 pickQty가 아니라 pickingQty를 표시기의 분류 수량으로 표시할 지 여부
	 * @return
	 */
	public static Map<String, List<IndicatorOnInformation>> buildIndOnList(
			boolean needUpdateJobStatus, String jobType, List<JobInstance> jobList, boolean showPickingQty) {
		
		if(ValueUtil.isNotEmpty(jobList)) {
			List<IndOnPickReq> indListToLightOn = new ArrayList<IndOnPickReq>(jobList.size());
			String pickStartedAt = needUpdateJobStatus ? DateUtil.currentTimeStr() : null;
			
			// 점등 요청을 위한 데이터 모델 생성.
			for(JobInstance job : jobList) {
				// 상태가 처리 예정인 작업만 표시기 점등 
				if(needUpdateJobStatus && job.isTodoJob()) {
					// 1. 분류 대상 피킹 시간 업데이트
					job.setPickStartedAt(pickStartedAt);
					// 2. 상태 코드 설정
					job.setStatus(LogisConstants.JOB_STATUS_PICKING);
				}
				
				// 3. 점등 요청 모델 생성 및 복사  
				IndOnPickReq lightOn = ValueUtil.populate(job, new IndOnPickReq(), "comCd", "inputSeq", "indCd", "colorCd", "pickQty", "boxInQty", "gwPath");
				// 4. 비지니스 ID 설정
				lightOn.setJobInstanceId(job.getId());
				// 5. pickingQty를 표시
				if(showPickingQty) {
					lightOn.setPickQty(job.getPickingQty());
				}
				// 6. 표시기 점등을 위한 리스트에 추가
				indListToLightOn.add(lightOn);
			}
			
			if(needUpdateJobStatus) {
				BeanUtil.get(IQueryManager.class).updateBatch(jobList, "status", "indCd", "pickStartedAt");
			}
			
			// 분류 대상 작업 데이터를 표시기 점등 요청을 위한 프로토콜 모델로 변환한다.
			return indListToLightOn.isEmpty() ? null : MwMessageUtil.groupPickingByGwPath(jobList.get(0).getDomainId(), jobType, indListToLightOn);
			
		} else {
			return null;
		}
	}

	/**
	 * 게이트웨이 리부팅시에 게이트웨이에 내려주기 위한 게이트웨이 소속 표시기 정보 리스트
	 * 
	 * @param gateway
	 * @return
	 */
	public static List<GatewayInitResIndList> indListForGwInit(Gateway gateway) {
		StringJoiner sql = new StringJoiner(SysConstants.LINE_SEPARATOR);
		sql.add("select distinct id, channel, pan, biz_type, nvl(view_type, '0') as view_type from (")
		   .add("	select x.mpi_cd as id, x.channel_no as channel, x.pan_no as pan, biz_type, ")
		   .add("		   (select value from tb_company_setting where domain_id = :domainId and name = ('com.mps.' || lower(biz_type) || '.mpi.view-type')) as view_type")
		   .add("	from (")
		   .add("   	select")
		   .add("   		nvl(decode(reg.job_type, 'DPS2', 'DAS2', reg.job_type), 'DAS') as biz_type, loc.loc_cd, loc.mpi_cd, loc.channel_no, loc.pan_no")
		   .add("   	from tb_location loc")
		   .add("   		 left outer join tb_region reg on loc.region_cd = reg.region_cd")
		   .add("   	where loc.domain_id = :domainId AND reg.domain_id = :domainId AND gw_zone_cd = :gwCd")
		   .add("	) x, (")
		   .add("   	select mpi_cd from tb_mpi where domain_id = :domainId and gw_cd = :gwCd")
		   .add("	) y")
		   .add("	where x.mpi_cd = y.mpi_cd")
		   .add("	order by x.loc_cd")
		   .add(")");
		
		Map<String, Object> params = ValueUtil.newMap("domainId,gwCd,gwPath", gateway.getDomainId(), gateway.getGwCd(), gateway.getGwNm());
		return BeanUtil.get(IQueryManager.class).selectListBySql(sql.toString(), params, GatewayInitResIndList.class, 0, 0);
	}
	
	/**
	 * Gateway에 소속된 표시기 정보 리스트
	 * 
	 * @param domainId
	 * @param gwPath
	 * @return
	 */
	public static List<GatewayInitResIndList> indList(Long domainId, String gwPath) {
		StringJoiner sql = new StringJoiner(SysConstants.LINE_SEPARATOR);
		sql.add("select distinct id, channel, pan, biz_type, nvl(view_type, '0') as view_type from (")
		   .add("	select x.mpi_cd as id, x.channel_no as channel, x.pan_no as pan, biz_type, (select value from tb_company_setting where domain_id = :domainId and name = ('com.mps.' || lower(biz_type) || '.mpi.view-type')) as view_type")
		   .add("	from (")
		   .add("   	select nvl(decode(reg.job_type, 'DPS2', 'DAS2', reg.job_type), 'DAS') as biz_type, loc.loc_cd, loc.mpi_cd, loc.channel_no, loc.pan_no")
		   .add("   	from tb_location loc")
		   .add("   		 left outer join tb_region reg on loc.region_cd = reg.region_cd")
		   .add("   	where loc.domain_id = :domainId AND reg.domain_id = :domainId AND loc.active_flag = 1")
		   .add("   		and gw_zone_cd = (")
		   .add("      			select gw_zone_cd from tb_gateway where domain_id = :domainId and gw_nm = :gwPath")
		   .add("   		)")
		   .add("	) x, (")
		   .add("   	select mpi_cd from tb_mpi where domain_id = :domainId and gw_cd = (")
		   .add("      		select gw_cd from tb_gateway where domain_id = :domainId and gw_nm = :gwPath")
		   .add("   	)")
		   .add("	) y")
		   .add("	where x.mpi_cd = y.mpi_cd")
		   .add("	order by x.loc_cd")
		   .add(")");
		
		Map<String, Object> params = ValueUtil.newMap("domainId,gwPath", domainId, gwPath);
		return BeanUtil.get(IQueryManager.class).selectListBySql(sql.toString(), params, GatewayInitResIndList.class, 0, 0);
	}

}