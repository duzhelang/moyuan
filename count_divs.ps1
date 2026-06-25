$lines = Get-Content 'd:\SOLO\SC_MoYuan2_\frontend\src\views\poem\list.vue'
$opens = 0
$closes = 0
for($i = 1747; $i -le 1850; $i++) {
    $line = $lines[$i]
    $openMatches = [regex]::Matches($line, '<div[\s>]')
    $closeMatches = [regex]::Matches($line, '</div>')
    if($openMatches.Count -gt 0 -or $closeMatches.Count -gt 0) {
        $opens += $openMatches.Count
        $closes += $closeMatches.Count
        $diff = $opens - $closes
        $lineNum = $i + 1
        $trimmed = $line.Trim()
        Write-Host "$lineNum : +$($openMatches.Count) -$($closeMatches.Count) (net: $diff) | $trimmed"
    }
}
Write-Host ""
Write-Host "Total opens: $opens, closes: $closes, diff: $($opens - $closes)"
