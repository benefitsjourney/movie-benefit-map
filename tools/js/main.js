const tools = {
  "excel-to-sql": {
    name: "Excel 轉 SQL",
    description: "批次將 Excel 轉換為 MSSQL INSERT 語法",
    source: "來源：data/*.xlsx",
    target: "輸出：benefit-backend/src/main/resources/db/data/",
    run: excelToSql,
  },
};

// 畫面元素
const tool = document.getElementById("tool");
const projectInput = document.getElementById("project");
const browse = document.getElementById("browse");
const button = document.getElementById("run");
const spinner = document.getElementById("spinner");
const runText = document.getElementById("runText");
const status = document.getElementById("status");
const description = document.getElementById("description");
const source = document.getElementById("source");
const target = document.getElementById("target");

let project = null;

/** 初始化工具選單 */
function initTools() {
  for (const [id, item] of Object.entries(tools)) {
    const option = document.createElement("option");
    option.value = id;
    option.textContent = item.name;
    tool.appendChild(option);
  }

  updateTool();
}

/** 更新工具說明 */
function updateTool() {
  const item = tools[tool.value];

  description.textContent = item.description;
  source.textContent = item.source;
  target.textContent = item.target;
}

/** 選擇專案根目錄 */
async function selectProject() {
  if (!window.showDirectoryPicker) {
    throw new Error(
      "目前瀏覽器不支援選擇資料夾功能，請使用最新版 Chrome 或 Edge。",
    );
  }

  const folder = await window.showDirectoryPicker({
    id: "benefits-journey",
    mode: "readwrite",
    ...(project ? { startIn: project } : {}),
  });

  // 確認選擇的是正確的專案
  try {
    await folder.getDirectoryHandle("data");
    await folder.getDirectoryHandle("benefit-backend");
  } catch {
    throw new Error("請選擇包含 data 與 benefit-backend 的專案根目錄");
  }

  project = folder;
  projectInput.value = folder.name;
  status.textContent = "專案資料夾已選擇";
}

/** 更新執行狀態 */
function setLoading(loading) {
  button.disabled = loading;
  browse.disabled = loading;
  tool.disabled = loading;

  spinner.classList.toggle("d-none", !loading);
  runText.textContent = loading ? "執行中..." : "執行工具";
}

/** 執行指定工具 */
async function runTool() {
  if (!project) {
    await Swal.fire("尚未選擇專案", "請先選擇專案根目錄", "warning");
    return;
  }

  const item = tools[tool.value];

  if (!item) {
    await Swal.fire("執行失敗", "找不到指定工具", "error");
    return;
  }

  setLoading(true);
  status.textContent = "正在執行...";

  // 顯示執行中的視窗
  Swal.fire({
    title: "正在執行工具",
    html: '<p id="progress">正在準備...</p>',
    allowOutsideClick: false,
    allowEscapeKey: false,
    showConfirmButton: false,
    didOpen: () => {
      Swal.showLoading();
    },
  });

  try {
    // 執行工具，並即時更新視窗訊息
    const result = await item.run(project, (message) => {
      status.textContent = message;

      const progress = Swal.getHtmlContainer()?.querySelector("#progress");

      if (progress) {
        progress.textContent = message;
      }
    });

    Swal.close();
    status.textContent = "執行完成";

    await Swal.fire({
      icon: "success",
      title: "執行完成",
      text: result.message,
    });
  } catch (error) {
    Swal.close();
    status.textContent = "執行失敗";

    await Swal.fire({
      icon: "error",
      title: "執行失敗",
      text: error.message,
    });
  } finally {
    setLoading(false);
  }
}

// 更換資料夾
browse.addEventListener("click", async () => {
  try {
    await selectProject();
  } catch (error) {
    if (error.name !== "AbortError") {
      Swal.fire("選擇失敗", error.message, "error");
    }
  }
});

// 選擇工具
tool.addEventListener("change", updateTool);

// 執行工具
button.addEventListener("click", runTool);

// 初始化
initTools();
