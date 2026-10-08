
/** camelCase 轉 snake_case */
function toSnakeCase(name) {
    return name
        .replace(/([a-z0-9])([A-Z])/g, "$1_$2")
        .toLowerCase();
}

/** Excel 日期轉 MSSQL 日期格式 */
function formatDate(date) {
    if (Number.isNaN(date.getTime())) {
        throw new Error("日期格式錯誤");
    }

    const pad = n => String(n).padStart(2, "0");

    return `${date.getUTCFullYear()}-${pad(date.getUTCMonth() + 1)}-${pad(date.getUTCDate())} ` +
        `${pad(date.getUTCHours())}:${pad(date.getUTCMinutes())}:${pad(date.getUTCSeconds())}`;
}

/** Excel 值轉 MSSQL 語法 */
function toSqlValue(value, column) {
    if (value === null || value === undefined || value === "") {
        return "NULL";
    }

    if (value instanceof Date) {
        return `N'${formatDate(value)}'`;
    }

    if (typeof value === "boolean") {
        return value ? "1" : "0";
    }

    if (typeof value === "number") {
        if (!Number.isFinite(value)) {
            throw new Error(`${column} 包含無效數字`);
        }

        return String(value);
    }

    const text = String(value).trim();

    // 空白字串
    if (text === "") {
        return "NULL";
    }

    // Boolean 欄位
    if (column.startsWith("is_") && /^(true|false|0|1)$/i.test(text)) {
        return /^(true|1)$/i.test(text) ? "1" : "0";
    }

    // 經緯度欄位
    if (["latitude", "longitude"].includes(column)
        && /^-?\d+(\.\d+)?$/.test(text)) {
        return text;
    }

    // Unicode 字串，跳脫單引號
    return `N'${text.replaceAll("'", "''")}'`;
}

/** 單份 Excel 轉 SQL */
async function convert(file, table) {
    const buffer = await file.arrayBuffer();

    const workbook = XLSX.read(buffer, {
        type: "array",
        cellDates: true
    });

    const sheet = workbook.Sheets[workbook.SheetNames[0]];

    if (!sheet || !sheet["!ref"]) {
        throw new Error(`${file.name} 沒有工作表或資料`);
    }

    const range = XLSX.utils.decode_range(sheet["!ref"]);

    // 固定從 Excel A 欄開始讀取
    range.s.c = 0;

    // 第一列為欄位名稱
    const data = XLSX.utils.sheet_to_json(sheet, {
        header: 1,
        range: range,
        defval: null,
        blankrows: true,
        raw: true
    });

    if (data.length < 2) {
        throw new Error(`${file.name} 沒有可匯入的資料`);
    }

    // 固定忽略 A 欄，從 B 欄開始讀取
    const headers = data[0]
        .slice(1)
        .map(name => String(name ?? "").trim());

    if (headers.length === 0) {
        throw new Error(`${file.name} 沒有可匯入的欄位`);
    }

    // 驗證欄位名稱
    if (headers.some(name => !name)) {
        throw new Error(`${file.name} 包含空白欄位名稱`);
    }

    const columns = headers.map(toSnakeCase);

    if (columns.some(col => !/^[a-z_][a-z0-9_]*$/.test(col))) {
        throw new Error(`${file.name} 包含不合法欄位名稱`);
    }

    if (new Set(columns).size !== columns.length) {
        throw new Error(`${file.name} 包含重複欄位`);
    }

    // 保留 Excel B 欄以後的原始索引
    const fields = columns.map((column, index) => ({
        column,
        index: index + 1
    }));

    const names = fields
        .map(field => `[${field.column}]`)
        .join(", ");

    const sql = [
        `-- ${table} 初始資料`,
        "-- A 欄主鍵由 SQL Server 自動產生",
        ""
    ];

    let count = 0;

    // 從第二列開始讀取資料
    for (let i = 1; i < data.length; i++) {
        const row = data[i] ?? [];
        const rowNumber = range.s.r + i + 1;

        // B 欄以後完全空白就跳過
        if (fields.every(field => {
            const value = row[field.index];

            return value === null
                || value === undefined
                || String(value).trim() === "";
        })) {
            continue;
        }

        // 驗證公式是否有計算結果
        for (const field of fields) {
            const address = XLSX.utils.encode_cell({
                r: rowNumber - 1,
                c: field.index
            });

            const cell = sheet[address];

            if (cell?.f && (cell.v === undefined || cell.v === null)) {
                throw new Error(
                    `${file.name} 第 ${rowNumber} 列 ${field.column} 公式沒有計算結果`
                );
            }
        }

        try {
            const values = fields.map(field =>
                toSqlValue(row[field.index], field.column)
            );

            sql.push(
                `INSERT INTO dbo.[${table}] (${names}) VALUES (${values.join(", ")});`
            );

            count++;

        } catch (error) {
            throw new Error(
                `${file.name} 第 ${rowNumber} 列：${error.message}`
            );
        }
    }

    if (count === 0) {
        throw new Error(`${file.name} 沒有可匯入的資料`);
    }

    return {
        content: sql.join("\n") + "\n",
        count
    };
}

/** 取得指定資料夾 */
async function getFolder(root, path, create = false) {
    let folder = root;

    for (const name of path) {
        folder = await folder.getDirectoryHandle(name, { create });
    }

    return folder;
}

/** 批次將所有 Excel 轉換為 SQL */
export async function excelToSql(project, progress = () => { }) {
    progress("正在讀取 Excel...");

    // Excel 來源資料夾
    const input = await getFolder(project, ["data"]);

    const results = [];
    const tables = new Set();

    // 第一階段：讀取並轉換所有 Excel
    for await (const [name, entry] of input.entries()) {
        if (entry.kind !== "file" || !name.toLowerCase().endsWith(".xlsx")) {
            continue;
        }

        // 排除 Excel 暫存檔
        if (name.startsWith("~$")) {
            continue;
        }

        // Excel 檔案名稱作為資料表名稱
        const table = name.slice(0, -5).toLowerCase();

        if (!/^[a-z][a-z0-9_]*$/.test(table)) {
            throw new Error(`不合法的資料表名稱：${table}`);
        }

        if (tables.has(table)) {
            throw new Error(`重複的資料表名稱：${table}`);
        }

        tables.add(table);

        progress(`正在轉換 ${name}...`);

        const file = await entry.getFile();
        const result = await convert(file, table);

        results.push({
            table,
            content: result.content,
            count: result.count
        });
    }

    if (results.length === 0) {
        throw new Error("data 資料夾沒有找到 .xlsx 檔案");
    }

    // 第二階段：所有 Excel 轉換成功後才寫入
    const output = await getFolder(project, [
        "benefit-backend",
        "src",
        "main",
        "resources",
        "db",
        "data"
    ], true);

    let rows = 0;

    for (const result of results) {
        progress(`正在寫入 ${result.table}.sql...`);

        const target = await output.getFileHandle(`${result.table}.sql`, {
            create: true
        });

        const writer = await target.createWritable();

        try {
            await writer.write(result.content);
        } finally {
            await writer.close();
        }

        rows += result.count;
    }

    return {
        message: `成功轉換 ${results.length} 個檔案，共 ${rows} 筆資料`
    };
}
