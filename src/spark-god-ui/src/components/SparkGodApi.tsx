import React, { useEffect, useState } from "react"
import * as RC from "recharts"            // React 19 + TS 경고 우회용

/* recharts 래퍼 */
const ResponsiveContainer = RC.ResponsiveContainer as unknown as React.FC<any>
const PieChart            = RC.PieChart            as unknown as React.FC<any>
const Pie                 = RC.Pie                 as unknown as React.FC<any>
const Cell                = RC.Cell                as unknown as React.FC<any>
const Tooltip             = RC.Tooltip             as unknown as React.FC<any>

/* ===== 타입 (원본 유지) ===== */
export interface ApplicationInfo { id:string; name:string; startTime:number; endTime:number; duration:number }
export interface EnvironmentInfo { sparkVersion:string; javaVersion:string; scalaVersion:string; osInfo:string; pythonVersion:string }
export interface SparkGodData { hello:string; application:ApplicationInfo; environment:EnvironmentInfo; timestamp:number }
export type ExecSummary = { totalExecutors:number; activeExecutors:number; totalCores:number; memUsedGiB:string; memMaxGiB:string }
export type DriverInfo  = { cores:number; memoryGiB:string; hostPort?:string; isActive?:boolean }
export type ExecutorRow = { id:string; isActive?:boolean; isDriver?:boolean; totalCores?:number; memoryUsed?:number; maxMemory?:number; hostPort?:string }
type KV=[string,string]
type EnvResponse={ runtime:unknown; sparkProperties:KV[]; systemProperties:KV[]; classpathEntries:KV[] }

/* ===== helpers ===== */
function getSparkProp(props: KV[], key: string){ return props.find(([k])=>k===key)?.[1] }
function parseMemoryToBytes(s?:string){ if(!s) return; const m=String(s).trim().match(/^(\d+(?:\.\d+)?)([kKmMgGtT])?b?$/); if(!m) return; const v=parseFloat(m[1]); const u=(m[2]||"").toLowerCase(); const p:{[k:string]:number}={ "":1,k:1024,m:1024**2,g:1024**3,t:1024**4 }; return Math.round(v*(p[u]??1)) }
function bytesToGiB(n?:number){ if(!n||!Number.isFinite(n)) return "0.0"; return (n/1024/1024/1024).toFixed(1) }
function getAppIdFromUrl(){ return window.location.pathname.match(/\/history\/([^/]+)/)?.[1] ?? null }
async function resolveAppAndAttempt(){
  const historyAppId=getAppIdFromUrl()
  if(historyAppId){
    const r=await fetch(`/api/v1/applications/${historyAppId}`); const data=await r.json()
    const attempts=Array.isArray(data)?(data[0]?.attempts??[]):(data.attempts??[])
    const attemptId=attempts.length?(attempts.at(-1).attemptId||""):""
    return {appId:historyAppId,attemptId}
  }
  const r=await fetch(`/api/v1/applications`); if(!r.ok) throw new Error(`Failed to list applications (${r.status})`)
  const arr=await r.json(); const first=Array.isArray(arr)?arr[0]:arr
  const appId=first?.id; const attempts=first?.attempts??[]; const attemptId=attempts.length?(attempts.at(-1).attemptId||""):""
  if(!appId) throw new Error("Cannot resolve appId."); return {appId,attemptId}
}

/* ===== Component ===== */
interface SparkGodApiProps { onExecutorsLoaded?: (executors: ExecutorRow[]) => void }

const SparkGodApi: React.FC<SparkGodApiProps> = ({ onExecutorsLoaded }) => {
  const [data,setData]=useState<SparkGodData|null>(null)
  const [loading,setLoading]=useState(true)
  const [error,setError]=useState<string|null>(null)
  const [execSummary,setExecSummary]=useState<ExecSummary|null>(null)
  const [driverInfo,setDriverInfo]=useState<DriverInfo|null>(null)
  const [execError,setExecError]=useState<string|null>(null)

  /* 기본 API */
  useEffect(()=>{ const run=async()=>{
      try{ setError(null)
        const res=await fetch("/spark-god/api/json/"); if(!res.ok) throw new Error(`HTTP ${res.status}`)
        const json:SparkGodData=await res.json(); setData(json)
      }catch(e:any){ setError(e.message||"Unknown error") } finally{ setLoading(false) }
    }; run(); const itv=setInterval(run,5000); return ()=>clearInterval(itv)
  },[])

  /* Executors / Driver */
  useEffect(()=>{ (async()=>{
      try{
        setExecError(null)
        const {appId,attemptId}=await resolveAppAndAttempt()
        const base=attemptId?`/api/v1/applications/${appId}/${attemptId}`:`/api/v1/applications/${appId}`
        const [execRes,envRes]=await Promise.all([ fetch(`${base}/executors`), fetch(`${base}/environment`) ])
        if(!execRes.ok) throw new Error(`Executors ${execRes.status}`); if(!envRes.ok) throw new Error(`Environment ${envRes.status}`)
        const executors:ExecutorRow[]=await execRes.json(); const envJson:EnvResponse=await envRes.json()
        onExecutorsLoaded?.(executors)

        const activeExecs=executors.filter(e=>e.id!=="driver"&&e.isActive)
        const totalCores=executors.reduce((s,e)=>s+(e.totalCores??0),0)
        const memUsed=executors.reduce((s,e)=>s+(e.memoryUsed??0),0)
        const memMax =executors.reduce((s,e)=>s+(e.maxMemory ??0),0)
        setExecSummary({ totalExecutors:executors.filter(e=>e.id!=="driver").length, activeExecutors:activeExecs.length,
          totalCores, memUsedGiB:bytesToGiB(memUsed), memMaxGiB:bytesToGiB(memMax) })

        const sp=envJson.sparkProperties||[]
        const driverCoresProp=getSparkProp(sp,"spark.driver.cores")
        const driverMemProp  =getSparkProp(sp,"spark.driver.memory")
        const driverRow=executors.find(e=>e.id==="driver"||e.isDriver)
        const driverCores=(driverCoresProp&&Number(driverCoresProp))||(driverRow?.totalCores??0)
        const driverMemBytes=parseMemoryToBytes(driverMemProp) ?? (driverRow?.maxMemory??0)
        setDriverInfo({ cores:driverCores, memoryGiB:bytesToGiB(driverMemBytes), hostPort:driverRow?.hostPort, isActive:driverRow?.isActive })
      }catch(e:any){ setExecSummary(null); setDriverInfo(null); setExecError(e.message||String(e)) }
    })()
  },[onExecutorsLoaded])

  const fmtTime=(ts:number)=>new Date(ts).toLocaleString()
  const fmtDur=(ms:number)=>{ const s=Math.floor(ms/1000), m=Math.floor(s/60), h=Math.floor(m/60); return h?`${h}h ${m%60}m ${s%60}s`:m?`${m}m ${s%60}s`:`${s}s` }

  if(loading) return <div className="py-8 text-center text-gray-600">Loading SparkGod API data...</div>
  if(error)   return <div className="py-8 text-center text-red-600">Error: {error}</div>
  if(!data)   return <div className="py-8 text-center">No data available</div>

  /* 파생값 */
  const memUsed=parseFloat(execSummary?.memUsedGiB ?? "0")
  const memMax =parseFloat(execSummary?.memMaxGiB  ?? "0")
  const memPct =memMax>0 ? Math.round((memUsed/memMax)*100) : 0

  const active =execSummary ? execSummary.activeExecutors : 0
  const inactive=execSummary ? Math.max(execSummary.totalExecutors-active,0) : 0
  const total  =active+inactive
  const activePct=total>0 ? Math.round((active/total)*100) : 0
  const pieData=[{name:"Active",value:active},{name:"Inactive",value:inactive}]
  const PIE=["#10b981","#e5e7eb"]
  const canChart=!!(RC && RC.PieChart)

  return (
    <div className="space-y-4">
      {/* 상단 메시지*/}
      <div className="card" style={{marginBottom:"1rem"}}>
        <strong style={{marginRight:6}}>🚀 Real-time Spark application monitoring & analytics made simple 📊</strong>
      </div>

      {/* KPI */}
      {execSummary && (
        <div className="kpi-grid" style={{marginBottom:"1rem"}}>
          <div className="kpi"><div className="label">Total Executors</div><div className="value">{execSummary.totalExecutors}</div></div>
          <div className="kpi"><div className="label">Active Executors</div><div className="value">{execSummary.activeExecutors}</div></div>
          <div className="kpi"><div className="label">Total Cores</div><div className="value">{execSummary.totalCores}</div></div>
          <div className="kpi"><div className="label">Memory Used</div><div className="value">{memUsed.toFixed(1)} / {memMax.toFixed(1)} GiB</div></div>
        </div>
      )}

      {/* 3열 레이아웃 */}
      <div className="grid-3">
        {/* 왼쪽: Application / Environment */}
        <div className="space-y-4">
          <div className="card">
            <h3>Application Info</h3>
            <div className="info-grid">
              <div className="k">ID:</div><div className="v">{data.application.id}</div>
              <div className="k">Name:</div><div className="v">{data.application.name}</div>
              <div className="k">Start Time:</div><div className="v">{fmtTime(data.application.startTime)}</div>
              <div className="k">End Time:</div><div className="v">{data.application.endTime>0?fmtTime(data.application.endTime):"Running"}</div>
              <div className="k">Duration:</div><div className="v">{fmtDur(data.application.duration)}</div>
            </div>
          </div>

          <div className="card">
            <h3>Environment Info</h3>
            <div className="info-grid">
              <div className="k">Spark Version:</div><div className="v">{data.environment.sparkVersion}</div>
              <div className="k">Java Version:</div><div className="v">{data.environment.javaVersion}</div>
              <div className="k">Scala Version:</div><div className="v">{data.environment.scalaVersion}</div>
              <div className="k">OS Info:</div><div className="v">{data.environment.osInfo}</div>
              <div className="k">Python Version:</div><div className="v">{data.environment.pythonVersion}</div>
            </div>
          </div>
        </div>

        {/* 가운데: Cluster Resources(프로그레스) + 도넛 */}
        <div className="space-y-4">
          <div className="card">
            <h3>Cluster Resources</h3>
            {/* Memory progress */}
            <div style={{marginBottom:"14px"}}>
              <div style={{display:"flex",justifyContent:"space-between",marginBottom:6}}>
                <span className="small">Memory usage</span>
                <span className="small">{memUsed.toFixed(1)} / {memMax.toFixed(1)} GiB ({memPct}%)</span>
              </div>
              <div className="progress"><span style={{width:`${Math.min(memPct,100)}%`}}/></div>
            </div>

            {/* Mini KPIs */}
            {execSummary && (
              <div style={{display:"grid",gridTemplateColumns:"repeat(2,minmax(0,1fr))",gap:"12px"}}>
                <div className="mini"><div className="l">Total Cores</div><div className="v">{execSummary.totalCores}</div></div>
                <div className="mini"><div className="l">Executors</div><div className="v">{execSummary.activeExecutors}/{execSummary.totalExecutors}</div></div>
              </div>
            )}
          </div>

          <div className="card">
            <h3>Executors State</h3>
            {canChart ? (
              <div className="chart-h" style={{position:"relative"}}>
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie data={pieData} dataKey="value" nameKey="name"
                         innerRadius={70} outerRadius={100}
                         startAngle={90} endAngle={-270} paddingAngle={2}>
                      {pieData.map((_,i)=><Cell key={i} fill={PIE[i%PIE.length]} />)}
                    </Pie>
                    <Tooltip />
                  </PieChart>
                </ResponsiveContainer>

                {/* 중앙 라벨 */}
                <div className="center-overlay">
                  <div className="chip">
                    <div style={{fontSize:"1.1rem",fontWeight:800}}>{activePct}%</div>
                    <div className="small">{active}/{total} active</div>
                  </div>
                </div>
              </div>
            ) : <div className="chart-h" style={{display:"flex",alignItems:"center",justifyContent:"center",border:"1px dashed var(--border)",borderRadius:12}}>Charts unavailable (install <code>recharts</code>).</div> }

            <div className="legend">
              <span><i style={{background:"#10b981"}}/>Active</span>
              <span><i style={{background:"#e5e7eb"}}/>Inactive</span>
            </div>
          </div>
        </div>

        {/* 오른쪽: Driver / Executors Summary / Updated */}
        <div className="space-y-4">
          {driverInfo && (
            <div className="card">
              <h3>Driver (History Server)</h3>
              <div className="info-grid">
                <div className="k">Cores:</div><div className="v">{driverInfo.cores}</div>
                <div className="k">Memory:</div><div className="v">{driverInfo.memoryGiB} GiB</div>
                {driverInfo.hostPort && (<><div className="k">Host:Port:</div><div className="v">{driverInfo.hostPort}</div></>)}
                {typeof driverInfo.isActive==="boolean" && (<>
                  <div className="k">Status:</div><div className="v">{driverInfo.isActive?"Active":"Inactive"}</div>
                </>)}
              </div>
            </div>
          )}

          {execSummary && (
            <div className="card">
              <h3>Executors (Summary)</h3>
              <div className="info-grid">
                <div className="k">Total Executors:</div><div className="v">{execSummary.totalExecutors}</div>
                <div className="k">Active Executors:</div><div className="v">{execSummary.activeExecutors}</div>
                <div className="k">Total Cores:</div><div className="v">{execSummary.totalCores}</div>
                <div className="k">Memory Used:</div><div className="v">{execSummary.memUsedGiB} GiB</div>
                <div className="k">Max Memory:</div><div className="v">{execSummary.memMaxGiB} GiB</div>
              </div>
            </div>
          )}

          <div className="card" style={{background:"#eef2ff",borderColor:"#e0e7ff",color:"#3730a3"}}>
            <strong style={{marginRight:6}}>Last Updated:</strong>{new Date(data.timestamp).toLocaleString()}
          </div>

          {execError && <div style={{color:"#dc2626"}}>{execError}</div>}
        </div>
      </div>
    </div>
  )
}

export default SparkGodApi
