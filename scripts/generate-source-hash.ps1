# 墨渊项目源程序文件哈希清单生成脚本
# 用途：为软件著作权/独创性证明生成源程序文件目录结构（每个文件附 SHA256 哈希值）
# 用法：powershell -ExecutionPolicy Bypass -File scripts/generate-source-hash.ps1
# 输出：docs/verification/source-hash-manifest.csv（清单） + source-hash-summary.json（统计摘要）

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$outDir = Join-Path $projectRoot "docs\verification"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

# 扫描范围：后端与前端源码（排除构建产物/依赖）
$scans = @(
    @{ Module = "backend";  Root = Join-Path $projectRoot "sc-moyuan-backend\src" }
    @{ Module = "frontend"; Root = Join-Path $projectRoot "frontend\src" }
)

$excludeDirNames = @("node_modules", "dist", "target", ".git", ".idea", "__pycache__")

$rows = [System.Collections.Generic.List[object]]::new()
$fileCount = 0
$totalLines = 0

foreach ($scan in $scans) {
    if (-not (Test-Path $scan.Root)) {
        Write-Host "警告：路径不存在，已跳过 -> $($scan.Root)" -ForegroundColor Yellow
        continue
    }
    $files = Get-ChildItem -Path $scan.Root -Recurse -File -ErrorAction SilentlyContinue |
        Where-Object {
            $dirOk = $true
            foreach ($ex in $excludeDirNames) {
                if ($_.FullName -match "\\$ex(\\|$)") { $dirOk = $false; break }
            }
            $dirOk -and $_.Extension -notin @(".map", ".png", ".jpg", ".jpeg", ".gif", ".svg", ".ico", ".woff", ".woff2", ".ttf", ".eot")
        }

    foreach ($f in $files) {
        $relPath = $f.FullName.Substring($scan.Root.Length).TrimStart('\')
        $hash = (Get-FileHash -Path $f.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
        $lines = 0
        try {
            $lines = (Get-Content -Path $f.FullName -ErrorAction Stop | Measure-Object -Line).Lines
        } catch {
            $lines = 0
        }
        $rows.Add([pscustomobject]@{
            Module   = $scan.Module
            Path     = $relPath
            SHA256   = $hash
            Lines    = $lines
            SizeKB   = [math]::Round($f.Length / 1KB, 2)
        })
        $fileCount++
        $totalLines += $lines
    }
}

$csvPath = Join-Path $outDir "source-hash-manifest.csv"
$rows | Sort-Object Module, Path | Export-Csv -Path $csvPath -NoTypeInformation -Encoding UTF8

$summary = [pscustomobject]@{
    GeneratedAt   = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
    FileCount     = $fileCount
    TotalLines    = $totalLines
    BackendFiles  = ($rows | Where-Object { $_.Module -eq "backend" }).Count
    FrontendFiles = ($rows | Where-Object { $_.Module -eq "frontend" }).Count
    Algorithm     = "SHA256"
}
$summary | ConvertTo-Json | Set-Content -Path (Join-Path $outDir "source-hash-summary.json") -Encoding UTF8

Write-Host "清单已生成：$csvPath" -ForegroundColor Green
Write-Host "文件总数：$fileCount，总行数：$totalLines"
