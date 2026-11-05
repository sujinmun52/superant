param([string]$In, [string]$Out)
$word = New-Object -ComObject Word.Application
$word.Visible = $false
$word.DisplayAlerts = 0
try {
    $doc = $word.Documents.Open($In, $false, $true)
    $doc.ExportAsFixedFormat($Out, 17)
    $doc.Close($false)
} finally {
    $word.Quit()
}
Write-Output "pdf: $Out"
