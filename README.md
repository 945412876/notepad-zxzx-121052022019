# Android记事本应用

这是一个基于Android的记事本应用，允许用户创建、编辑和管理笔记。该应用使用NoSQL数据库存储数据，为用户生成的内容提供灵活、可扩展的存储解决方案。

## 功能

### 1. **NoteList界面显示时间戳**
   - **功能描述**：NoteList界面现在为每个笔记条目显示时间戳。
   - **实现方式**：当笔记被创建或修改时，应用会自动生成并存储时间戳。这个时间戳会在主界面的笔记列表中显示。
 #### 截图展示：
   ![NoteList显示时间戳](images/0011.png)
 #### 代码实现：
  ![NoteList显示时间戳](images/0012.png)
 ### 2. **排序功能**
   - **功能描述**：笔记现在可以按时间（时间顺序）或标题（字母顺序）进行排序。
   - **实现方式**：用户可以在NoteList界面中的菜单中选择排序方式，笔记会根据选定的方式重新排列，按创建或修改时间排序，或按标题字母排序。
 #### 截图展示：
   ![排序](images/002.png)
      ![排序（按时间戳）](images/0021.png)
        ![排序(按照字符)](images/0022.png)
        #### 代码实现：（```java）
### 3. **笔记查询功能**
   - **功能描述**：新增了按标题或内容搜索笔记的功能。
   - **实现方式**：用户可以在搜索框中输入关键字，应用会根据标题或内容与搜索词匹配的笔记进行筛选，方便用户快速找到特定笔记。
   -  #### 截图展示：
      ![查询](images/0031.png)
     ![查询](images/0032.png)
 #### 代码实现：
### 4. **更改记事本背景**
   - **功能描述**：用户现在可以更改记事本界面的背景。
   - **实现方式**：在设置或选项菜单中，用户可以选择不同的背景主题或图片，使记事本界面更加个性化。
 #### 截图展示：
   ![NoteList更改记事本背景（弹出选项）](images/0041.png)
      ![NoteList更改记事本背景（颜色选择界面）](images/0042.png)
         ![NoteList更改记事本背景（点击红色）](images/0043.png)
            ![NoteList更改记事本背景（点击紫色）](images/0041.png)
             #### 代码实现：
```(private void updateBackgroundColor(String color) {
        findViewById(R.id.noteEditorLayout).setBackgroundColor(Color.parseColor(color));
    }
    private void showColorPickerDialog() {
        // 定义颜色名称数组和对应的 Hex 值
        final String[] colorNames = {
                "Red", "Green", "Blue", "Yellow", "Purple", "Orange",
                "Pink", "Cyan", "Magenta", "Brown", "Grey", "Teal"
        };

        final String[] colorHexValues = {
                "#FF0000", "#00FF00", "#0000FF", "#FFFF00", "#800080", "#FFA500", // 红、绿、蓝、黄、紫、橙
                "#FFC0CB", "#00FFFF", "#FF00FF", "#A52A2A", "#808080", "#008080"  // 粉红、青、品红、棕、灰、青绿色
        };

        // 创建一个GridView的适配器，显示每个颜色块
        GridView gridView = new GridView(this);
        gridView.setNumColumns(2); // 设置为2列显示
        gridView.setVerticalSpacing(10);
        gridView.setHorizontalSpacing(10);
        gridView.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);

        // 使用颜色 Hex 值来创建一个自定义的颜色选择适配器
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, colorNames) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                // 使用自定义的布局来显示颜色块
                if (convertView == null) {
                    convertView = new View(getContext());
                }

                // 设置颜色块的背景为对应的 Hex 值
                convertView.setBackgroundColor(Color.parseColor(colorHexValues[position]));
                convertView.setLayoutParams(new GridView.LayoutParams(150, 150)); // 设置颜色块的大小
                return convertView;
            }
        };

        gridView.setAdapter(adapter);

        // 创建AlertDialog，显示GridView
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择背景颜色");
        builder.setView(gridView);

        // 设置GridView的点击事件监听器
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 获取用户选择的颜色的 Hex 值
                String newColorHex = colorHexValues[position];

                // 更新背景颜色
                updateBackgroundColor(newColorHex);

                // 将背景颜色更新到数据库
                ContentValues values = new ContentValues();
                values.put(NotePad.Notes.COLUMN_NAME_BACKGROUND_COLOR, newColorHex);
                getContentResolver().update(mUri, values, null, null);
            }
        });

        builder.show();
    }
)
