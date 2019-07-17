package xyz.anythings.gw.service.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import xyz.anythings.base.LogisBaseConstants;
import xyz.anythings.base.entity.JobProcess;
import xyz.anythings.base.entity.Location;
import xyz.anythings.gw.LogisGwConstants;
import xyz.anythings.gw.entity.Gateway;
import xyz.anythings.gw.entity.MPI;
import xyz.anythings.gw.model.IndicatorOnInformation;
import xyz.anythings.gw.service.MpiSendService;
import xyz.anythings.gw.service.model.MpiCommonReq;
import xyz.anythings.gw.service.model.MpiOnPickReq;
import xyz.anythings.sys.util.AnyOrmUtil;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.sys.SysConstants;
import xyz.elidom.sys.util.DateUtil;
import xyz.elidom.sys.util.ValueUtil;
import xyz.elidom.util.BeanUtil;

/**
 * MPS Service Utilities
 * 
 * @author shortstop
 */
public class MpsServiceUtil {
	
	/**
	 * 호기내 로케이션들 중에 거래처 매핑된 
	 * 
	 * @param domainId
	 * @param jobType
	 * @param mpiList
	 */
	public static int mpiOnNoboxDisplay(Long domainId, String jobType, List<MpiCommonReq> mpiList) {
		// 1. 빈 값 체크 
		if(ValueUtil.isNotEmpty(mpiList)) {
			MpiSendService sendSvc = BeanUtil.get(MpiSendService.class);
			
			// 2. 점등 요청을 위한 데이터 모델 생성. 
			Map<String, List<IndicatorOnInformation>> indOnList = new HashMap<String, List<IndicatorOnInformation>>();

			for (MpiCommonReq mpiOnPick : mpiList) {
				String gwPath = mpiOnPick.getGwPath();
				List<IndicatorOnInformation> mpiOnList = indOnList.containsKey(gwPath) ? indOnList.get(gwPath) : new ArrayList<IndicatorOnInformation>();
				IndicatorOnInformation mpiOnInfo = new IndicatorOnInformation();
				mpiOnInfo.setId(mpiOnPick.getMpiCd());
				mpiOnInfo.setBizId(mpiOnPick.getMpiCd());
				mpiOnList.add(mpiOnInfo);
				indOnList.put(gwPath, mpiOnList);
			}
					
			if(ValueUtil.isNotEmpty(indOnList)) {
				// 3. 표시기 점등 요청
				sendSvc.requestMpisOn(domainId, jobType, LogisGwConstants.MPI_ACTION_TYPE_NOBOX, indOnList);
				// 4. 점등된 표시기 개수 리턴 
				return indOnList.size();
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
	public static void restoreMpiDisplayJobPicked(String jobType, List<JobProcess> jobList) {
		
		if(ValueUtil.isNotEmpty(jobList)) {
			MpiSendService sendSvc = BeanUtil.get(MpiSendService.class);
			
			// 점등 요청을 위한 데이터 모델 생성. 
			Map<String, List<IndicatorOnInformation>> indOnList = new HashMap<String, List<IndicatorOnInformation>>();
			Long domainId = null;
			
			for (JobProcess job : jobList) {
				if(domainId == null) domainId = job.getDomainId();
				String gwPath = job.getGwPath();
				List<IndicatorOnInformation> mpiOnList = indOnList.containsKey(gwPath) ? indOnList.get(gwPath) : new ArrayList<IndicatorOnInformation>();
				IndicatorOnInformation mpiOnInfo = new IndicatorOnInformation();
				mpiOnInfo.setId(job.getMpiCd());
				mpiOnInfo.setBizId(job.getId());
				mpiOnInfo.setOrgEaQty(job.getPickedQty());
				mpiOnList.add(mpiOnInfo);
				indOnList.put(gwPath, mpiOnList);
			}
			
			// 표시기 점등 요청
			if(ValueUtil.isNotEmpty(indOnList)) {
				sendSvc.requestMpisOn(domainId, jobType, LogisGwConstants.MPI_ACTION_TYPE_DISPLAY, indOnList);
			}
		}
	}
	
	/**
	 * 작업이 완료된 표시기에 END 표시를 복원
	 * 
	 * @param domainId
	 * @param jobType
	 * @param gateway
	 * @return
	 */
	public static List<Location> restoreMpiDisplayBoxingEnd(Long domainId, String jobType, Gateway gateway) {
		// 1. DAS, RTN에 대해서 로케이션의 jobStatus가 END, ENDED 상태인 모든 로케이션을 조회
		Query condition = AnyOrmUtil.newConditionForExecution(domainId, 0, 0, "domain_id", "loc_cd", "mpi_cd", "job_status", "job_process_id");
		condition.addFilter("mpiCd", SysConstants.IN, gateway.mpiCdList());
		condition.addFilter("jobStatus", SysConstants.IN, LogisBaseConstants.LOCATION_JOB_STATUS_END_LIST);
		condition.addOrder("locCd", true);
		List<Location> locations = BeanUtil.get(IQueryManager.class).selectList(Location.class, condition);
		
		// 2. 로케이션 별로 상태별로 END (ReadOnly = false), END (ReadOnly = true)를 표시
		return restoreMpiDisplayBoxingEnd(jobType, locations);
	}
	
	/**
	 * 호기, 작업 존에 작업이 완료된 표시기에 END 표시를 복원
	 * 
	 * @param domainId
	 * @param jobType
	 * @param regionCd
	 * @param equipZoneCd
	 * @return
	 */
	public static List<Location> restoreMpiDisplayBoxingEnd(Long domainId, String jobType, String regionCd, String equipZoneCd) {
		// 1. DAS, RTN에 대해서 로케이션의 jobStatus가 END, ENDED 상태인 모든 로케이션을 조회
		Query condition = AnyOrmUtil.newConditionForExecution(domainId, 0, 0,  "domain_id", "loc_cd", "mpi_cd", "job_status", "job_process_id");
		condition.addFilter("regionCd", regionCd);
		condition.addFilter("jobStatus", SysConstants.IN, LogisBaseConstants.LOCATION_JOB_STATUS_END_LIST);
		condition.addOrder("locCd", true);
		List<Location> locations = BeanUtil.get(IQueryManager.class).selectList(Location.class, condition);
		
		// 2. 로케이션 별로 상태별로 END (ReadOnly = false), END (ReadOnly = true)를 표시
		return restoreMpiDisplayBoxingEnd(jobType, locations);
	}
	
	/**
	 * 로케이션 별로 상태별로 END (ReadOnly = false), END (ReadOnly = true)를 표시
	 * 
	 * @param jobType
	 * @param locations
	 * @return
	 */
	public static List<Location> restoreMpiDisplayBoxingEnd(String jobType, List<Location> locations) {
		if(ValueUtil.isNotEmpty(locations)) {
			MpiSendService mpiSendService = BeanUtil.get(MpiSendService.class);

			for(Location loc : locations) {
				String jobStatus = loc.getJobStatus();
				
				if(ValueUtil.isNotEmpty(jobStatus)) {
					if(ValueUtil.isEqual(LogisBaseConstants.LOCATION_JOB_STATUS_END, jobStatus)) {
						String bizId = ValueUtil.isEmpty(loc.getJobProcessId()) ? loc.getMpiCd() : loc.getJobProcessId();
						mpiSendService.requestMpiEndDisplay(loc.getDomainId(), jobType, loc.getMpiCd(), bizId, false);
						
					} else if(ValueUtil.isEqual(LogisBaseConstants.LOCATION_JOB_STATUS_ENDED, jobStatus)) {
						String bizId = ValueUtil.isEmpty(loc.getJobProcessId()) ? loc.getMpiCd() : loc.getJobProcessId();
						mpiSendService.requestMpiEndDisplay(loc.getDomainId(), jobType, loc.getMpiCd(), bizId, true);
					}
				}			
			}
		}
		
		return locations;
	}
	
	/**
	 * 작업 데이터로 표시기를 점등한다.
	 * 
	 * @param needUpdateJobStatus 
	 * @param job
	 * @return
	 */
	public static boolean mpiOnByJob(boolean needUpdateJobStatus, JobProcess job) {
		if(ValueUtil.isEmpty(job.getGwPath())) {
			String gwPath = MPI.findGatewayPath(job.getDomainId(), job.getMpiCd());
			job.setGwPath(gwPath);
		}
		
		List<JobProcess> jobList = ValueUtil.toList(job);
		MpsServiceUtil.mpiOnByJobList(needUpdateJobStatus, job.getJobType(), jobList);
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
	public static boolean mpiOnByJob(boolean needUpdateJobStatus, JobProcess job, boolean showPickingQty) {
		if(ValueUtil.isEmpty(job.getGwPath())) {
			String gwPath = MPI.findGatewayPath(job.getDomainId(), job.getMpiCd());
			job.setGwPath(gwPath);
		}
		
		List<JobProcess> jobList = ValueUtil.toList(job);
		MpsServiceUtil.mpiOnByJobList(needUpdateJobStatus, job.getJobType(), jobList, showPickingQty);
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
	public static int mpiOnByJobList(boolean needUpdateJobStatus, String jobType, List<JobProcess> jobList) {
		// 1. 빈 값 체크 
		if(ValueUtil.isNotEmpty(jobList)) {
			// 2. 점등 요청을 위한 데이터 모델 생성. 
			Map<String, List<IndicatorOnInformation>> mpiOnList = 
					buildMpiOnList(needUpdateJobStatus, jobType, jobList, false);
			
			if(ValueUtil.isNotEmpty(mpiOnList)) {
				JobProcess firstJob = jobList.get(0);
				// 3. 표시기 점등 요청
				BeanUtil.get(MpiSendService.class).requestMpisOn(firstJob.getDomainId(), jobType, LogisGwConstants.MPI_ACTION_TYPE_PICK, mpiOnList);
				// 4. 점등된 표시기 개수 리턴 
				return mpiOnList.size();
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
	public static int mpiOnByJobList(boolean needUpdateJobStatus, String jobType, List<JobProcess> jobList, boolean showPickingQty) {
		// 1. 빈 값 체크 
		if(ValueUtil.isNotEmpty(jobList)) {
			// 2. 점등 요청을 위한 데이터 모델 생성. 
			Map<String, List<IndicatorOnInformation>> mpiOnList = 
					buildMpiOnList(needUpdateJobStatus, jobType, jobList, true);
			
			if(ValueUtil.isNotEmpty(mpiOnList)) {
				JobProcess firstJob = jobList.get(0);
				// 3. 표시기 점등 요청
				BeanUtil.get(MpiSendService.class).requestMpisOn(firstJob.getDomainId(), jobType, LogisGwConstants.MPI_ACTION_TYPE_PICK, mpiOnList);
				// 4. 점등된 표시기 개수 리턴 
				return mpiOnList.size();
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
	public static int mpiOnByPickingJobList(boolean needUpdateJobStatus, boolean qytNoCheck, String jobType, List<JobProcess> jobList) {
		// 1. 빈 값 체크 
		if(ValueUtil.isNotEmpty(jobList)) {
			List<JobProcess> pickingJobs = new ArrayList<JobProcess>(jobList.size());
			
			for(JobProcess job : jobList) {
				// 피킹 예정 수량이 피킹 확정 수량보다 큰 것만 표시기 점등 
				if(qytNoCheck || (job.getPickQty() > job.getPickedQty())) {
					pickingJobs.add(job);
				}
			}
			
			if(ValueUtil.isNotEmpty(pickingJobs)) {
	 			// 2. 점등 요청을 위한 데이터 모델 생성. 
				Map<String, List<IndicatorOnInformation>> mpiOnList = 
						buildMpiOnList(needUpdateJobStatus, jobType, jobList, false);
				if(ValueUtil.isNotEmpty(mpiOnList)) {
					JobProcess firstJob = pickingJobs.get(0);
					// 3. 표시기 점등 요청
					BeanUtil.get(MpiSendService.class).requestMpisOn(firstJob.getDomainId(), jobType, LogisGwConstants.MPI_ACTION_TYPE_PICK, mpiOnList);
					// 4. 점등된 표시기 개수 리턴 
					return mpiOnList.size();
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
	public static int mpiDisplayByPickingJobList(boolean needUpdateJobStatus, boolean qytNoCheck, String jobType, List<JobProcess> jobList) {
		// 1. 빈 값 체크 
		if(ValueUtil.isNotEmpty(jobList)) {
			List<JobProcess> pickingJobs = new ArrayList<JobProcess>(jobList.size());
			
			for(JobProcess job : jobList) {
				// 피킹 예정 수량이 피킹 확정 수량보다 큰 것만 표시기 점등 
				if(qytNoCheck || (job.getPickQty() > job.getPickedQty())) {
					pickingJobs.add(job);
				}
			}
			
			if(ValueUtil.isNotEmpty(pickingJobs)) {
	 			// 2. 점등 요청을 위한 데이터 모델 생성. 
				Map<String, List<IndicatorOnInformation>> mpiOnList = 
						buildMpiOnList(needUpdateJobStatus, jobType, jobList, false);
				if(ValueUtil.isNotEmpty(mpiOnList)) {
					JobProcess firstJob = pickingJobs.get(0);
					// 3. 표시기 점등 요청
					BeanUtil.get(MpiSendService.class).requestMpisOn(firstJob.getDomainId(), jobType, LogisGwConstants.MPI_ACTION_TYPE_DISPLAY, mpiOnList);
					// 4. 점등된 표시기 개수 리턴 
					return mpiOnList.size();
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
	public static int mpiOnByInspectJobList(String jobType, List<JobProcess> jobList) {
		// 1. 빈 값 체크 
		if(ValueUtil.isNotEmpty(jobList)) {
			Long domainId = null;
			
			// 2. 검수 색깔은 빨간색으로 고정
			for(JobProcess job : jobList) {
				if(domainId == null) domainId = job.getDomainId();
				job.setMpiColor(LogisGwConstants.COLOR_RED);
			}
			
			// 3. 점등 요청을 위한 데이터 모델 생성. 
			Map<String, List<IndicatorOnInformation>> mpiOnList = buildMpiOnList(false, jobType, jobList, false);
			
			if(ValueUtil.isNotEmpty(mpiOnList)) {
				// 4. 표시기 점등 요청
				BeanUtil.get(MpiSendService.class).requestMpisInspectOn(domainId, jobType, mpiOnList);
				// 5. 점등된 표시기 개수 리턴 
				return mpiOnList.size();
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
	public static Map<String, List<IndicatorOnInformation>> buildMpiOnList(
			boolean needUpdateJobStatus, String jobType, List<JobProcess> jobList, boolean showPickingQty) {
		
		if(ValueUtil.isNotEmpty(jobList)) {
			List<MpiOnPickReq> mpiListToLightOn = new ArrayList<MpiOnPickReq>(jobList.size());
			String pickStartedAt = needUpdateJobStatus ? DateUtil.currentTimeStr() : null;
			
			// 점등 요청을 위한 데이터 모델 생성.
			for(JobProcess job : jobList) {
				// 상태가 처리 예정인 작업만 표시기 점등 
				if(needUpdateJobStatus && job.isTodoJob()) {
					// 1. 분류 대상 피킹 시간 업데이트
					job.setPickStartedAt(pickStartedAt);
					// 2. 상태 코드 설정
					job.setStatus(JobProcess.JOB_STATUS_PICKING);
				}
				
				// 3. 점등 요청 모델 생성 및 복사  
				MpiOnPickReq lightOn = ValueUtil.populate(job, new MpiOnPickReq(), "comCd", "processSeq", "mpiCd", "mpiColor", "pickQty", "boxInQty", "gwPath");
				// 4. 비지니스 ID 설정
				lightOn.setJobProcessId(job.getId());
				// 5. pickingQty를 표시
				if(showPickingQty) {
					lightOn.setPickQty(job.getPickingQty());
				}
				// 6. 표시기 점등을 위한 리스트에 추가
				mpiListToLightOn.add(lightOn);
			}
			
			if(needUpdateJobStatus) {
				BeanUtil.get(IQueryManager.class).updateBatch(jobList, "status", "mpiCd", "pickStartedAt");
			}
			
			// 분류 대상 작업 데이터를 표시기 점등 요청을 위한 프로토콜 모델로 변환한다.
			return mpiListToLightOn.isEmpty() ? null : MwMessageUtil.groupPickingByGwPath(jobList.get(0).getDomainId(), jobType, mpiListToLightOn);
			
		} else {
			return null;
		}
	}

}
