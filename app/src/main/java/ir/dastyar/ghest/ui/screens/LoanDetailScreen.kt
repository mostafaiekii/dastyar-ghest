package ir.dastyar.ghest.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.dastyar.ghest.data.Installment
import ir.dastyar.ghest.data.Loan
import ir.dastyar.ghest.data.LoanType
import ir.dastyar.ghest.ui.components.DueChip
import ir.dastyar.ghest.ui.components.JalaliDateField
import ir.dastyar.ghest.ui.components.MoneyField
import ir.dastyar.ghest.ui.components.ProgressRing
import ir.dastyar.ghest.util.Farsi
import ir.dastyar.ghest.util.Jalali
import ir.dastyar.ghest.util.PhotoStore
import ir.dastyar.ghest.viewmodel.LoanDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun LoanDetailScreen(
    vm: LoanDetailViewModel = viewModel(),
    onBack: () -> Unit
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val today = remember { Jalali.todayEpochDay() }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showEditLoan by remember { mutableStateOf(false) }
    var editInst by remember { mutableStateOf<Installment?>(null) }
    val loan = state.loan

    Column(Modifier.fillMaxSize()) {
        // نوار بالا
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "بازگشت")
            }
            Text(
                loan?.title ?: "جزئیات",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "بیشتر")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("ویرایش مشخصات") },
                        leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                        onClick = { showEditLoan = true; menuOpen = false }
                    )
                    DropdownMenuItem(
                        text = { Text(if (loan?.archived == true) "خروج از بایگانی" else "بایگانی") },
                        leadingIcon = { Icon(Icons.Rounded.Archive, contentDescription = null) },
                        onClick = { vm.archive(loan?.archived != true); menuOpen = false }
                    )
                    DropdownMenuItem(
                        text = { Text("حذف کامل") },
                        leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { confirmDelete = true; menuOpen = false }
                    )
                }
            }
        }

        if (loan == null) {
            Text(
                "این وام پیدا نشد",
                Modifier.padding(24.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Column
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        Modifier.padding(18.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                LoanType.label(loan.type) + if (loan.creditor.isNotBlank()) " • " + loan.creditor else "",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                Farsi.money(state.paid) + " از " + Farsi.money(state.total),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "مانده: " + Farsi.money(state.total - state.paid),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            if (loan.note.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    loan.note,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        ProgressRing(
                            progress = state.progress,
                            color = MaterialTheme.colorScheme.primary,
                            track = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
                        ) {
                            Text(
                                Farsi.digit((state.progress * 100).toInt()) + "٪",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // ---------- عکس مدرک (دور ۲) ----------
            item {
                PhotoCard(
                    photoName = loan.photoUri,
                    loanId = loan.id,
                    onPhotoSaved = { vm.setPhoto(it) }
                )
            }

            if (state.overdueCount > 0) {
                item {
                    Button(
                        onClick = { vm.payAllOverdue() },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(
                            "پرداخت همه‌ی " + Farsi.digit(state.overdueCount) + " قسط عقب‌افتاده",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            items(state.installments, key = { it.id }) { inst ->
                InstallmentRow(
                    number = inst.number,
                    due = inst.dueEpochDay,
                    amount = inst.amount,
                    isPaid = inst.isPaid,
                    today = today,
                    onToggle = { vm.togglePaid(inst) },
                    onEdit = { editInst = inst }
                )
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("حذف وام") },
            text = { Text("همه‌ی اقساط این وام هم حذف می‌شود. مطمئنی؟") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.delete(onDone = onBack) }) {
                    Text("حذف", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("انصراف") } }
        )
    }

    if (showEditLoan && loan != null) {
        EditLoanSheet(
            loan = loan,
            onDismiss = { showEditLoan = false },
            onSave = { title, creditor, type, amount, note ->
                vm.updateLoanMeta(title, creditor, type, amount, note)
                showEditLoan = false
            }
        )
    }

    editInst?.let { inst ->
        EditInstallmentDialog(
            inst = inst,
            onDismiss = { editInst = null },
            onSave = { due, amount ->
                vm.editInstallment(inst, due, amount)
                editInst = null
            }
        )
    }
}

/* ---------------- ردیف قسط با دکمه ویرایش ---------------- */

@Composable
private fun InstallmentRow(
    number: Int,
    due: Long,
    amount: Long,
    isPaid: Boolean,
    today: Long,
    onToggle: () -> Unit,
    onEdit: () -> Unit
) {
    val overdue = !isPaid && due < today
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (overdue) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = if (isPaid) MaterialTheme.colorScheme.primary
                else if (overdue) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    Farsi.digit(number),
                    Modifier.padding(8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = if (isPaid) MaterialTheme.colorScheme.onPrimary
                    else if (overdue) MaterialTheme.colorScheme.onError
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    Jalali.format(due),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (isPaid) "پرداخت‌شده" else Farsi.money(amount),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            if (!isPaid) DueChip(due, today)
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Rounded.Edit,
                    contentDescription = "ویرایش قسط",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onToggle) {
                Icon(
                    if (isPaid) Icons.Rounded.CheckCircle else Icons.Rounded.Check,
                    contentDescription = if (isPaid) "لغو پرداخت" else "پرداخت شد",
                    tint = if (isPaid) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/* ---------------- عکس مدرک (دور ۲) ---------------- */

@Composable
private fun PhotoCard(
    photoName: String?,
    loanId: Long,
    onPhotoSaved: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var captureFile by remember { mutableStateOf<File?>(null) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var saving by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(photoName) {
        preview = photoName?.let {
            withContext(Dispatchers.IO) { PhotoStore.loadPreview(context, it) }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok ->
        val file = captureFile
        if (file == null) return@rememberLauncherForActivityResult
        if (ok) {
            scope.launch {
                saving = true
                errorMsg = null
                val name = withContext(Dispatchers.IO) {
                    PhotoStore.saveCompressed(
                        context,
                        PhotoStore.uriForFile(context, file),
                        "loan-$loanId.jpg"
                    )
                }
                withContext(Dispatchers.IO) { file.delete() }
                saving = false
                if (name != null) {
                    onPhotoSaved(name)
                } else {
                    errorMsg = "ذخیره‌ی عکس ناموفق بود؛ دوباره امتحان کن"
                }
            }
        } else {
            file.delete()
        }
    }

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.CameraAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "عکس مدرک",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                if (preview != null) {
                    TextButton(onClick = {
                        val file = PhotoStore.newCaptureFile(context)
                        captureFile = file
                        cameraLauncher.launch(PhotoStore.uriForFile(context, file))
                    }) { Text("عکس جدید") }
                    TextButton(onClick = {
                        photoName?.let { PhotoStore.delete(context, it) }
                        onPhotoSaved(null)
                    }) {
                        Text("حذف", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (saving) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text(
                    "در حال ذخیره‌ی عکس...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (preview != null) {
                Image(
                    bitmap = preview!!.asImageBitmap(),
                    contentDescription = "عکس مدرک وام",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "عکس فقط روی حافظه‌ی خود گوشی ذخیره می‌شود.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                OutlinedButton(
                    onClick = {
                        val file = PhotoStore.newCaptureFile(context)
                        captureFile = file
                        cameraLauncher.launch(PhotoStore.uriForFile(context, file))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.CameraAlt, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("عکس‌برداری از برگه‌ی وام / قسط")
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "عکس چک یا برگه‌ی اقساط را نگه‌دار تا هر وقت خواستی بدون جست‌وجوی کاغذها، اعداد را ببینی. بدون نیاز به مجوز دوربین.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            errorMsg?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/* ---------------- ویرایش مشخصات وام (دور ۲) ---------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditLoanSheet(
    loan: Loan,
    onDismiss: () -> Unit,
    onSave: (title: String, creditor: String, type: String, amount: Long, note: String) -> Unit
) {
    var title by remember { mutableStateOf(loan.title) }
    var creditor by remember { mutableStateOf(loan.creditor) }
    var type by remember { mutableStateOf(loan.type) }
    var amountText by remember { mutableStateOf(Farsi.digit(loan.installmentAmount)) }
    var note by remember { mutableStateOf(loan.note) }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text("ویرایش مشخصات وام", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("عنوان") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = creditor, onValueChange = { creditor = it },
                label = { Text("طرف حساب (بانک / شخص)") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))

            Text("نوع", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(LoanType.all.size) { idx ->
                    val t = LoanType.all[idx]
                    FilterChip(
                        selected = type == t,
                        onClick = { type = t },
                        label = { Text(LoanType.label(t)) }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))

            MoneyField(amountText, { amountText = it }, "مبلغ هر قسط")
            Spacer(Modifier.height(6.dp))
            Text(
                "تغییر مبلغ روی همه‌ی اقساط پرداخت‌نشده هم اعمال می‌شود.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = note, onValueChange = { note = it },
                label = { Text("یادداشت") },
                modifier = Modifier.fillMaxWidth(), minLines = 1, maxLines = 3
            )

            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val amount = Farsi.parseNumber(amountText)
                    when {
                        title.isBlank() -> error = "عنوان را بنویس"
                        amount <= 0 -> error = "مبلغ قسط را وارد کن"
                        else -> onSave(title, creditor, type, amount, note)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("ذخیره تغییرات", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/* ---------------- ویرایش یک قسط (دور ۲) ---------------- */

@Composable
private fun EditInstallmentDialog(
    inst: Installment,
    onDismiss: () -> Unit,
    onSave: (due: Long, amount: Long) -> Unit
) {
    var due by remember { mutableStateOf<Long?>(inst.dueEpochDay) }
    var amountText by remember { mutableStateOf(Farsi.digit(inst.amount)) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ویرایش قسط " + Farsi.digit(inst.number)) },
        text = {
            Column {
                JalaliDateField(label = "تاریخ سررسید", epochDay = due, onPick = { due = it })
                Spacer(Modifier.height(10.dp))
                MoneyField(amountText, { amountText = it }, "مبلغ قسط")
                if (inst.isPaid) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "این قسط پرداخت‌شده است؛ تغییر مبلغ در گزارش‌ها هم لحاظ می‌شود.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amount = Farsi.parseNumber(amountText)
                val d = due
                when {
                    d == null -> error = "تاریخ سررسید را انتخاب کن"
                    amount <= 0 -> error = "مبلغ قسط را وارد کن"
                    else -> onSave(d, amount)
                }
            }) { Text("ذخیره", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
