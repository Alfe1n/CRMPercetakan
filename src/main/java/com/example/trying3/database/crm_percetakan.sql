-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: localhost:3306
-- Generation Time: Oct 24, 2025 at 07:18 PM
-- Server version: 8.0.30
-- PHP Version: 8.1.10

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `crm_percetakan`
--

DELIMITER $$
--
-- Procedures
--
CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_generate_laporan_mingguan` (IN `p_tanggal_mulai` DATE, IN `p_tanggal_selesai` DATE, IN `p_generated_by` INT)   BEGIN
    DECLARE v_total_pesanan INT;
    DECLARE v_total_pendapatan DECIMAL(15,2);
    
    -- Hitung statistik
    SELECT COUNT(*), COALESCE(SUM(total_biaya), 0)
    INTO v_total_pesanan, v_total_pendapatan
    FROM pesanan
    WHERE tanggal_pesanan BETWEEN p_tanggal_mulai AND p_tanggal_selesai;
    
    -- Insert ke tabel laporan
    INSERT INTO laporan (
        jenis_laporan,
        tanggal_mulai,
        tanggal_selesai,
        total_pesanan,
        total_pendapatan,
        generated_by
    ) VALUES (
        'mingguan',
        p_tanggal_mulai,
        p_tanggal_selesai,
        v_total_pesanan,
        v_total_pendapatan,
        p_generated_by
    );
    
    -- Return ID laporan yang baru dibuat
    SELECT LAST_INSERT_ID() AS id_laporan;
END$$

CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_get_pesanan_follow_up` ()   BEGIN
    SELECT 
        p.id_pesanan,
        p.nomor_pesanan,
        pl.nama AS nama_pelanggan,
        pl.no_telepon,
        sp.nama_status,
        p.tanggal_pesanan,
        DATEDIFF(CURDATE(), p.tanggal_pesanan) AS hari_berlalu,
        CASE 
            WHEN sp.nama_status = 'Menunggu Pembayaran' AND DATEDIFF(CURDATE(), p.tanggal_pesanan) > 3 
                THEN 'Pembayaran tertunda >3 hari'
            WHEN sp.nama_status = 'Menunggu Persetujuan Desain' AND DATEDIFF(CURDATE(), p.tanggal_pesanan) > 5 
                THEN 'Desain belum disetujui >5 hari'
            WHEN p.deadline < CURDATE() AND sp.nama_status != 'Selesai' 
                THEN 'Melewati deadline'
            ELSE 'Normal'
        END AS alasan_follow_up
    FROM pesanan p
    JOIN pelanggan pl ON p.id_pelanggan = pl.id_pelanggan
    JOIN status_pesanan sp ON p.id_status = sp.id_status
    HAVING alasan_follow_up != 'Normal'
    ORDER BY p.tanggal_pesanan ASC;
END$$

DELIMITER ;

-- --------------------------------------------------------

--
-- Table structure for table `connection_log`
--

CREATE TABLE `connection_log` (
  `id_log` int NOT NULL,
  `status` enum('online','offline') NOT NULL,
  `timestamp` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `duration_seconds` int DEFAULT NULL COMMENT 'Berapa lama dalam status tersebut'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `desain`
--

CREATE TABLE `desain` (
  `id_desain` int NOT NULL,
  `id_pesanan` int NOT NULL,
  `id_designer` int NOT NULL COMMENT 'User yang mengerjakan desain',
  `id_status_desain` int NOT NULL,
  `file_desain_path` varchar(255) DEFAULT NULL,
  `revisi_ke` int DEFAULT '1',
  `tanggal_dibuat` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `tanggal_disetujui` datetime DEFAULT NULL,
  `catatan` text COMMENT 'Instruksi atau feedback',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `detail_pesanan`
--

CREATE TABLE `detail_pesanan` (
  `id_detail` int NOT NULL,
  `id_pesanan` int NOT NULL,
  `id_layanan` int NOT NULL,
  `jumlah` int NOT NULL DEFAULT '1',
  `harga_satuan` decimal(15,2) NOT NULL,
  `subtotal` decimal(15,2) NOT NULL,
  `spesifikasi` text COMMENT 'Ukuran, warna, finishing, dll',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Triggers `detail_pesanan`
--
DELIMITER $$
CREATE TRIGGER `trg_after_delete_detail_pesanan` AFTER DELETE ON `detail_pesanan` FOR EACH ROW BEGIN
    UPDATE pesanan 
    SET total_biaya = (
        SELECT COALESCE(SUM(subtotal), 0) 
        FROM detail_pesanan 
        WHERE id_pesanan = OLD.id_pesanan
    )
    WHERE id_pesanan = OLD.id_pesanan;
END
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_after_insert_detail_pesanan` AFTER INSERT ON `detail_pesanan` FOR EACH ROW BEGIN
    UPDATE pesanan 
    SET total_biaya = (
        SELECT COALESCE(SUM(subtotal), 0) 
        FROM detail_pesanan 
        WHERE id_pesanan = NEW.id_pesanan
    )
    WHERE id_pesanan = NEW.id_pesanan;
END
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_after_update_detail_pesanan` AFTER UPDATE ON `detail_pesanan` FOR EACH ROW BEGIN
    UPDATE pesanan 
    SET total_biaya = (
        SELECT COALESCE(SUM(subtotal), 0) 
        FROM detail_pesanan 
        WHERE id_pesanan = NEW.id_pesanan
    )
    WHERE id_pesanan = NEW.id_pesanan;
END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Table structure for table `jenis_layanan`
--

CREATE TABLE `jenis_layanan` (
  `id_layanan` int NOT NULL,
  `nama_layanan` varchar(100) NOT NULL,
  `deskripsi` text,
  `harga_dasar` decimal(15,2) NOT NULL DEFAULT '0.00',
  `satuan` varchar(20) DEFAULT 'pcs' COMMENT 'pcs, lembar, set, dll',
  `is_active` tinyint(1) DEFAULT '1',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `kendala_produksi`
--

CREATE TABLE `kendala_produksi` (
  `id_kendala` int NOT NULL,
  `id_produksi` int NOT NULL,
  `id_kendala_type` int DEFAULT NULL,
  `deskripsi` text NOT NULL,
  `solusi` text,
  `status` enum('open','in_progress','resolved') DEFAULT 'open',
  `dilaporkan_oleh` int NOT NULL,
  `tanggal_lapor` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `tanggal_selesai` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `laporan`
--

CREATE TABLE `laporan` (
  `id_laporan` int NOT NULL,
  `jenis_laporan` enum('mingguan','bulanan','kustom') NOT NULL,
  `tanggal_mulai` date NOT NULL,
  `tanggal_selesai` date NOT NULL,
  `file_path` varchar(255) DEFAULT NULL COMMENT 'Path ke file Excel/PDF',
  `total_pesanan` int DEFAULT '0',
  `total_pendapatan` decimal(15,2) DEFAULT '0.00',
  `generated_by` int NOT NULL,
  `generated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `log_aktivitas`
--

CREATE TABLE `log_aktivitas` (
  `id_log` int NOT NULL,
  `id_user` int NOT NULL COMMENT 'User yang melakukan aksi',
  `aksi` varchar(255) NOT NULL COMMENT 'login, logout, create_pesanan, dll',
  `tabel_terkait` varchar(50) DEFAULT NULL COMMENT 'Nama tabel yang diubah',
  `id_record` int DEFAULT NULL COMMENT 'ID record yang diubah',
  `detail_perubahan` text COMMENT 'JSON data sebelum/sesudah',
  `ip_address` varchar(45) DEFAULT NULL COMMENT 'IP address user',
  `user_agent` varchar(255) DEFAULT NULL COMMENT 'Browser/device info',
  `timestamp` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `master_kendala`
--

CREATE TABLE `master_kendala` (
  `id_kendala_type` int NOT NULL,
  `nama_kendala` varchar(100) NOT NULL,
  `kategori` enum('mesin','bahan','tenaga_kerja','lainnya') NOT NULL,
  `solusi_umum` text COMMENT 'Panduan penanganan',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `metode_pembayaran`
--

CREATE TABLE `metode_pembayaran` (
  `id_metode` int NOT NULL,
  `nama_metode` varchar(50) NOT NULL,
  `tipe` enum('bank_transfer','e_wallet','cash') NOT NULL,
  `nomor_rekening` varchar(50) DEFAULT NULL COMMENT 'Untuk bank_transfer',
  `atas_nama` varchar(100) DEFAULT NULL COMMENT 'Nama penerima',
  `logo_path` varchar(255) DEFAULT NULL COMMENT 'Path logo payment method',
  `is_active` tinyint(1) DEFAULT '1',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `pelanggan`
--

CREATE TABLE `pelanggan` (
  `id_pelanggan` int NOT NULL,
  `nama` varchar(100) NOT NULL,
  `email` varchar(100) DEFAULT NULL,
  `no_telepon` varchar(20) NOT NULL,
  `alamat` text,
  `media_komunikasi` enum('whatsapp','email','telepon','website') DEFAULT 'whatsapp',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `pembayaran`
--

CREATE TABLE `pembayaran` (
  `id_pembayaran` int NOT NULL,
  `id_pesanan` int NOT NULL,
  `id_metode` int NOT NULL,
  `jumlah` decimal(15,2) NOT NULL,
  `status_pembayaran` enum('pending','verified','failed','cancelled') DEFAULT 'pending',
  `bukti_pembayaran_path` varchar(255) DEFAULT NULL COMMENT 'Path ke file bukti transfer',
  `tanggal_pembayaran` datetime NOT NULL,
  `tanggal_verifikasi` datetime DEFAULT NULL,
  `verified_by` int DEFAULT NULL COMMENT 'User yang verifikasi',
  `catatan` text,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `pesanan`
--

CREATE TABLE `pesanan` (
  `id_pesanan` int NOT NULL,
  `nomor_pesanan` varchar(50) NOT NULL COMMENT 'Format: PO-YYYYMMDD-0001',
  `id_pelanggan` int NOT NULL,
  `id_user_admin` int NOT NULL COMMENT 'Admin yang input',
  `id_status` int NOT NULL,
  `tanggal_pesanan` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deadline` date DEFAULT NULL COMMENT 'Target selesai',
  `catatan` text,
  `total_biaya` decimal(15,2) DEFAULT '0.00',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Triggers `pesanan`
--
DELIMITER $$
CREATE TRIGGER `trg_after_update_status_pesanan` AFTER UPDATE ON `pesanan` FOR EACH ROW BEGIN
    IF OLD.id_status != NEW.id_status THEN
        INSERT INTO log_aktivitas (
            id_user, 
            aksi, 
            tabel_terkait, 
            id_record, 
            detail_perubahan
        ) VALUES (
            NEW.id_user_admin,
            'update_status_pesanan',
            'pesanan',
            NEW.id_pesanan,
            CONCAT('Status berubah dari ', 
                   (SELECT nama_status FROM status_pesanan WHERE id_status = OLD.id_status),
                   ' ke ',
                   (SELECT nama_status FROM status_pesanan WHERE id_status = NEW.id_status))
        );
    END IF;
END
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_before_insert_pesanan` BEFORE INSERT ON `pesanan` FOR EACH ROW BEGIN
    DECLARE next_num INT;
    DECLARE today VARCHAR(8);
    
    SET today = DATE_FORMAT(CURDATE(), '%Y%m%d');
    
    SELECT COALESCE(MAX(CAST(SUBSTRING(nomor_pesanan, 12) AS UNSIGNED)), 0) + 1
    INTO next_num
    FROM pesanan
    WHERE nomor_pesanan LIKE CONCAT('PO-', today, '%');
    
    SET NEW.nomor_pesanan = CONCAT('PO-', today, '-', LPAD(next_num, 4, '0'));
END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Table structure for table `produksi`
--

CREATE TABLE `produksi` (
  `id_produksi` int NOT NULL,
  `id_pesanan` int NOT NULL,
  `id_operator` int NOT NULL COMMENT 'User produksi yang handle',
  `tanggal_mulai` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `tanggal_selesai` datetime DEFAULT NULL,
  `progres_persen` int DEFAULT '0',
  `status_produksi` enum('antrian','proses','selesai','terkendala') DEFAULT 'antrian',
  `mesin_digunakan` varchar(100) DEFAULT NULL COMMENT 'Nama mesin cetak',
  `catatan` text,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ;

-- --------------------------------------------------------

--
-- Table structure for table `revisi_desain`
--

CREATE TABLE `revisi_desain` (
  `id_revisi` int NOT NULL,
  `id_desain` int NOT NULL,
  `revisi_ke` int NOT NULL,
  `file_path` varchar(255) NOT NULL,
  `catatan_revisi` text,
  `direvisi_oleh` int NOT NULL COMMENT 'Designer yang revisi',
  `tanggal_revisi` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `role`
--

CREATE TABLE `role` (
  `id_role` int NOT NULL,
  `nama_role` varchar(50) NOT NULL,
  `deskripsi` text NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `role`
--

INSERT INTO `role` (`id_role`, `nama_role`, `deskripsi`, `created_at`) VALUES
(1, 'Admin', 'Deskripsi admin', '2025-10-13 05:19:47'),
(2, 'Design', 'Deskripsi Designer', '2025-10-13 05:19:47'),
(3, 'Management', 'Deskirpsi management', '2025-10-13 05:19:47'),
(4, 'Produksi', 'Deskripsi Produksi', '2025-10-13 05:19:47');

-- --------------------------------------------------------

--
-- Table structure for table `status_desain`
--

CREATE TABLE `status_desain` (
  `id_status_desain` int NOT NULL,
  `nama_status` varchar(50) NOT NULL,
  `deskripsi` text,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `status_pesanan`
--

CREATE TABLE `status_pesanan` (
  `id_status` int NOT NULL,
  `nama_status` varchar(50) NOT NULL,
  `urutan` int NOT NULL COMMENT 'Urutan tahapan proses',
  `warna_badge` varchar(20) DEFAULT NULL COMMENT 'Untuk UI: success, warning, danger, info',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `sync_queue`
--

CREATE TABLE `sync_queue` (
  `id_sync` int NOT NULL,
  `table_name` varchar(50) NOT NULL COMMENT 'Tabel yang diubah',
  `operation` enum('INSERT','UPDATE','DELETE') NOT NULL,
  `record_id` int NOT NULL COMMENT 'ID record yang diubah',
  `data_json` text NOT NULL COMMENT 'Snapshot data dalam JSON',
  `synced` tinyint(1) DEFAULT '0',
  `sync_priority` int DEFAULT '1' COMMENT '1=low, 5=high',
  `created_by` int NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `synced_at` timestamp NULL DEFAULT NULL,
  `error_message` text COMMENT 'Jika gagal sync',
  `retry_count` int DEFAULT '0'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `user`
--

CREATE TABLE `user` (
  `id_user` int NOT NULL,
  `username` varchar(50) NOT NULL,
  `password_hash` varchar(255) NOT NULL COMMENT 'BCrypt hash',
  `email` varchar(100) NOT NULL,
  `nama_lengkap` varchar(100) NOT NULL,
  `id_role` int NOT NULL,
  `is_active` tinyint(1) DEFAULT '1',
  `last_login` datetime DEFAULT NULL,
  `failed_login_attempts` int DEFAULT '0',
  `locked_until` datetime DEFAULT NULL COMMENT 'Auto unlock setelah waktu ini',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `user`
--

INSERT INTO `user` (`id_user`, `username`, `password_hash`, `email`, `nama_lengkap`, `id_role`, `is_active`, `last_login`, `failed_login_attempts`, `locked_until`, `created_at`, `updated_at`) VALUES
(1, 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMye5lxrp4lJH8FxKbUxDXdqWpFKGdXqjQu', 'admin@percetakan.com', 'Administrator', 1, 1, NULL, 5, '2025-10-25 01:56:14', '2025-10-24 17:02:24', '2025-10-24 18:26:14'),
(2, 'designer1', '$2a$10$N9qo8uLOickgx2ZMRZoMye5lxrp4lJH8FxKbUxDXdqWpFKGdXqjQu', 'designer@percetakan.com', 'Budi Designer', 2, 1, NULL, 0, NULL, '2025-10-24 17:02:39', '2025-10-24 17:02:39'),
(3, 'operator1', '$2a$10$N9qo8uLOickgx2ZMRZoMye5lxrp4lJH8FxKbUxDXdqWpFKGdXqjQu', 'operator@percetakan.com', 'Joko Operator', 3, 1, NULL, 0, NULL, '2025-10-24 17:02:39', '2025-10-24 17:02:39'),
(5, 'admin1', '$2a$10$Xe.PQK9pZY5L5K5L5K5L5O5K5L5K5L5K5L5K5L5K5L5K5L5K5L5K5K', 'admin1@percetakan.com', 'Administrator Sistem', 1, 1, NULL, 1, NULL, '2025-10-24 18:53:34', '2025-10-24 19:16:40'),
(6, 'Admin01', '$2a$10$8v9lXlU70XjcA4mtberhzu4YLpthbsiaz.KEsdFZoyaHzO9HP9TNu', 'Admin@gmai.com', 'Haerul Akbar', 1, 1, '2025-10-25 02:17:56', 0, NULL, '2025-10-24 19:12:57', '2025-10-24 19:17:56');

-- --------------------------------------------------------

--
-- Stand-in structure for view `v_dashboard_stats`
-- (See below for the actual view)
--
CREATE TABLE `v_dashboard_stats` (
`pesanan_hari_ini` bigint
,`pesanan_minggu_ini` bigint
,`pesanan_selesai` bigint
,`pembayaran_pending` bigint
,`pendapatan_bulan_ini` decimal(37,2)
,`produksi_berjalan` bigint
,`kendala_aktif` bigint
);

-- --------------------------------------------------------

--
-- Stand-in structure for view `v_pesanan_lengkap`
-- (See below for the actual view)
--
CREATE TABLE `v_pesanan_lengkap` (
`id_pesanan` int
,`nomor_pesanan` varchar(50)
,`tanggal_pesanan` datetime
,`deadline` date
,`nama_pelanggan` varchar(100)
,`no_telepon` varchar(20)
,`admin_input` varchar(100)
,`nama_status` varchar(50)
,`urutan_status` int
,`total_biaya` decimal(15,2)
,`jumlah_item` bigint
,`total_dibayar` decimal(37,2)
,`sisa_pembayaran` decimal(38,2)
);

-- --------------------------------------------------------

--
-- Stand-in structure for view `v_produksi_alert`
-- (See below for the actual view)
--
CREATE TABLE `v_produksi_alert` (
`id_produksi` int
,`nomor_pesanan` varchar(50)
,`nama_pelanggan` varchar(100)
,`operator` varchar(100)
,`progres_persen` int
,`status_produksi` enum('antrian','proses','selesai','terkendala')
,`tanggal_mulai` datetime
,`hari_berjalan` int
,`deadline` date
,`hari_tersisa` int
,`alert_type` varchar(15)
);

-- --------------------------------------------------------

--
-- Structure for view `v_dashboard_stats`
--
DROP TABLE IF EXISTS `v_dashboard_stats`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `v_dashboard_stats`  AS SELECT (select count(0) from `pesanan` where (cast(`pesanan`.`tanggal_pesanan` as date) = curdate())) AS `pesanan_hari_ini`, (select count(0) from `pesanan` where (week(`pesanan`.`tanggal_pesanan`,0) = week(curdate(),0))) AS `pesanan_minggu_ini`, (select count(0) from `pesanan` where `pesanan`.`id_status` in (select `status_pesanan`.`id_status` from `status_pesanan` where (`status_pesanan`.`nama_status` = 'Selesai'))) AS `pesanan_selesai`, (select count(0) from `pembayaran` where (`pembayaran`.`status_pembayaran` = 'pending')) AS `pembayaran_pending`, (select coalesce(sum(`pesanan`.`total_biaya`),0) from `pesanan` where (month(`pesanan`.`tanggal_pesanan`) = month(curdate()))) AS `pendapatan_bulan_ini`, (select count(0) from `produksi` where (`produksi`.`status_produksi` = 'proses')) AS `produksi_berjalan`, (select count(0) from `kendala_produksi` where (`kendala_produksi`.`status` = 'open')) AS `kendala_aktif` ;

-- --------------------------------------------------------

--
-- Structure for view `v_pesanan_lengkap`
--
DROP TABLE IF EXISTS `v_pesanan_lengkap`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `v_pesanan_lengkap`  AS SELECT `p`.`id_pesanan` AS `id_pesanan`, `p`.`nomor_pesanan` AS `nomor_pesanan`, `p`.`tanggal_pesanan` AS `tanggal_pesanan`, `p`.`deadline` AS `deadline`, `pl`.`nama` AS `nama_pelanggan`, `pl`.`no_telepon` AS `no_telepon`, `u`.`nama_lengkap` AS `admin_input`, `sp`.`nama_status` AS `nama_status`, `sp`.`urutan` AS `urutan_status`, `p`.`total_biaya` AS `total_biaya`, count(`dp`.`id_detail`) AS `jumlah_item`, coalesce(sum((case when (`pm`.`status_pembayaran` = 'verified') then `pm`.`jumlah` else 0 end)),0) AS `total_dibayar`, (`p`.`total_biaya` - coalesce(sum((case when (`pm`.`status_pembayaran` = 'verified') then `pm`.`jumlah` else 0 end)),0)) AS `sisa_pembayaran` FROM (((((`pesanan` `p` join `pelanggan` `pl` on((`p`.`id_pelanggan` = `pl`.`id_pelanggan`))) join `user` `u` on((`p`.`id_user_admin` = `u`.`id_user`))) join `status_pesanan` `sp` on((`p`.`id_status` = `sp`.`id_status`))) left join `detail_pesanan` `dp` on((`p`.`id_pesanan` = `dp`.`id_pesanan`))) left join `pembayaran` `pm` on((`p`.`id_pesanan` = `pm`.`id_pesanan`))) GROUP BY `p`.`id_pesanan` ;

-- --------------------------------------------------------

--
-- Structure for view `v_produksi_alert`
--
DROP TABLE IF EXISTS `v_produksi_alert`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `v_produksi_alert`  AS SELECT `pr`.`id_produksi` AS `id_produksi`, `p`.`nomor_pesanan` AS `nomor_pesanan`, `pl`.`nama` AS `nama_pelanggan`, `u`.`nama_lengkap` AS `operator`, `pr`.`progres_persen` AS `progres_persen`, `pr`.`status_produksi` AS `status_produksi`, `pr`.`tanggal_mulai` AS `tanggal_mulai`, (to_days(curdate()) - to_days(`pr`.`tanggal_mulai`)) AS `hari_berjalan`, `p`.`deadline` AS `deadline`, (to_days(`p`.`deadline`) - to_days(curdate())) AS `hari_tersisa`, (case when (`pr`.`status_produksi` = 'terkendala') then 'Terkendala' when ((to_days(`p`.`deadline`) - to_days(curdate())) < 2) then 'Deadline Dekat' when (((to_days(curdate()) - to_days(`pr`.`tanggal_mulai`)) > 5) and (`pr`.`progres_persen` < 50)) then 'Progress Lambat' else 'Normal' end) AS `alert_type` FROM (((`produksi` `pr` join `pesanan` `p` on((`pr`.`id_pesanan` = `p`.`id_pesanan`))) join `pelanggan` `pl` on((`p`.`id_pelanggan` = `pl`.`id_pelanggan`))) join `user` `u` on((`pr`.`id_operator` = `u`.`id_user`))) WHERE (`pr`.`status_produksi` <> 'selesai') HAVING (`alert_type` <> 'Normal') ;

--
-- Indexes for dumped tables
--

--
-- Indexes for table `connection_log`
--
ALTER TABLE `connection_log`
  ADD PRIMARY KEY (`id_log`),
  ADD KEY `idx_timestamp` (`timestamp`);

--
-- Indexes for table `desain`
--
ALTER TABLE `desain`
  ADD PRIMARY KEY (`id_desain`),
  ADD KEY `idx_pesanan` (`id_pesanan`),
  ADD KEY `idx_designer` (`id_designer`),
  ADD KEY `idx_status` (`id_status_desain`);

--
-- Indexes for table `detail_pesanan`
--
ALTER TABLE `detail_pesanan`
  ADD PRIMARY KEY (`id_detail`),
  ADD KEY `id_layanan` (`id_layanan`),
  ADD KEY `idx_pesanan` (`id_pesanan`);

--
-- Indexes for table `jenis_layanan`
--
ALTER TABLE `jenis_layanan`
  ADD PRIMARY KEY (`id_layanan`);

--
-- Indexes for table `kendala_produksi`
--
ALTER TABLE `kendala_produksi`
  ADD PRIMARY KEY (`id_kendala`),
  ADD KEY `id_kendala_type` (`id_kendala_type`),
  ADD KEY `dilaporkan_oleh` (`dilaporkan_oleh`),
  ADD KEY `idx_produksi` (`id_produksi`),
  ADD KEY `idx_status` (`status`);

--
-- Indexes for table `laporan`
--
ALTER TABLE `laporan`
  ADD PRIMARY KEY (`id_laporan`),
  ADD KEY `generated_by` (`generated_by`),
  ADD KEY `idx_tanggal` (`tanggal_mulai`,`tanggal_selesai`),
  ADD KEY `idx_jenis` (`jenis_laporan`);

--
-- Indexes for table `log_aktivitas`
--
ALTER TABLE `log_aktivitas`
  ADD PRIMARY KEY (`id_log`),
  ADD KEY `idx_user` (`id_user`),
  ADD KEY `idx_timestamp` (`timestamp`),
  ADD KEY `idx_aksi` (`aksi`),
  ADD KEY `idx_log_user_timestamp` (`id_user`,`timestamp`);

--
-- Indexes for table `master_kendala`
--
ALTER TABLE `master_kendala`
  ADD PRIMARY KEY (`id_kendala_type`);

--
-- Indexes for table `metode_pembayaran`
--
ALTER TABLE `metode_pembayaran`
  ADD PRIMARY KEY (`id_metode`);

--
-- Indexes for table `pelanggan`
--
ALTER TABLE `pelanggan`
  ADD PRIMARY KEY (`id_pelanggan`),
  ADD KEY `idx_nama` (`nama`),
  ADD KEY `idx_telepon` (`no_telepon`);
ALTER TABLE `pelanggan` ADD FULLTEXT KEY `idx_fulltext_search` (`nama`,`email`,`no_telepon`);

--
-- Indexes for table `pembayaran`
--
ALTER TABLE `pembayaran`
  ADD PRIMARY KEY (`id_pembayaran`),
  ADD KEY `id_metode` (`id_metode`),
  ADD KEY `verified_by` (`verified_by`),
  ADD KEY `idx_pesanan` (`id_pesanan`),
  ADD KEY `idx_status` (`status_pembayaran`),
  ADD KEY `idx_tanggal` (`tanggal_pembayaran`),
  ADD KEY `idx_pembayaran_pesanan_status` (`id_pesanan`,`status_pembayaran`);

--
-- Indexes for table `pesanan`
--
ALTER TABLE `pesanan`
  ADD PRIMARY KEY (`id_pesanan`),
  ADD UNIQUE KEY `nomor_pesanan` (`nomor_pesanan`),
  ADD KEY `id_user_admin` (`id_user_admin`),
  ADD KEY `idx_nomor` (`nomor_pesanan`),
  ADD KEY `idx_pelanggan` (`id_pelanggan`),
  ADD KEY `idx_status` (`id_status`),
  ADD KEY `idx_tanggal` (`tanggal_pesanan`),
  ADD KEY `idx_pesanan_tanggal_status` (`tanggal_pesanan`,`id_status`);
ALTER TABLE `pesanan` ADD FULLTEXT KEY `idx_fulltext_catatan` (`catatan`);

--
-- Indexes for table `produksi`
--
ALTER TABLE `produksi`
  ADD PRIMARY KEY (`id_produksi`),
  ADD KEY `idx_pesanan` (`id_pesanan`),
  ADD KEY `idx_operator` (`id_operator`),
  ADD KEY `idx_status` (`status_produksi`),
  ADD KEY `idx_produksi_status_progres` (`status_produksi`,`progres_persen`);

--
-- Indexes for table `revisi_desain`
--
ALTER TABLE `revisi_desain`
  ADD PRIMARY KEY (`id_revisi`),
  ADD KEY `direvisi_oleh` (`direvisi_oleh`),
  ADD KEY `idx_desain` (`id_desain`);

--
-- Indexes for table `role`
--
ALTER TABLE `role`
  ADD PRIMARY KEY (`id_role`),
  ADD UNIQUE KEY `nama_role` (`nama_role`);

--
-- Indexes for table `status_desain`
--
ALTER TABLE `status_desain`
  ADD PRIMARY KEY (`id_status_desain`),
  ADD UNIQUE KEY `nama_status` (`nama_status`);

--
-- Indexes for table `status_pesanan`
--
ALTER TABLE `status_pesanan`
  ADD PRIMARY KEY (`id_status`),
  ADD UNIQUE KEY `nama_status` (`nama_status`);

--
-- Indexes for table `sync_queue`
--
ALTER TABLE `sync_queue`
  ADD PRIMARY KEY (`id_sync`),
  ADD KEY `created_by` (`created_by`),
  ADD KEY `idx_synced` (`synced`),
  ADD KEY `idx_table` (`table_name`),
  ADD KEY `idx_created_at` (`created_at`);

--
-- Indexes for table `user`
--
ALTER TABLE `user`
  ADD PRIMARY KEY (`id_user`),
  ADD UNIQUE KEY `username` (`username`),
  ADD UNIQUE KEY `email` (`email`),
  ADD KEY `id_role` (`id_role`),
  ADD KEY `idx_username` (`username`),
  ADD KEY `idx_email` (`email`),
  ADD KEY `idx_active` (`is_active`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `connection_log`
--
ALTER TABLE `connection_log`
  MODIFY `id_log` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `desain`
--
ALTER TABLE `desain`
  MODIFY `id_desain` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `detail_pesanan`
--
ALTER TABLE `detail_pesanan`
  MODIFY `id_detail` int NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `jenis_layanan`
--
ALTER TABLE `jenis_layanan`
  MODIFY `id_layanan` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `kendala_produksi`
--
ALTER TABLE `kendala_produksi`
  MODIFY `id_kendala` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `laporan`
--
ALTER TABLE `laporan`
  MODIFY `id_laporan` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `log_aktivitas`
--
ALTER TABLE `log_aktivitas`
  MODIFY `id_log` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `master_kendala`
--
ALTER TABLE `master_kendala`
  MODIFY `id_kendala_type` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `metode_pembayaran`
--
ALTER TABLE `metode_pembayaran`
  MODIFY `id_metode` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `pelanggan`
--
ALTER TABLE `pelanggan`
  MODIFY `id_pelanggan` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `pembayaran`
--
ALTER TABLE `pembayaran`
  MODIFY `id_pembayaran` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `pesanan`
--
ALTER TABLE `pesanan`
  MODIFY `id_pesanan` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `produksi`
--
ALTER TABLE `produksi`
  MODIFY `id_produksi` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `revisi_desain`
--
ALTER TABLE `revisi_desain`
  MODIFY `id_revisi` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `role`
--
ALTER TABLE `role`
  MODIFY `id_role` int NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT for table `status_desain`
--
ALTER TABLE `status_desain`
  MODIFY `id_status_desain` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `status_pesanan`
--
ALTER TABLE `status_pesanan`
  MODIFY `id_status` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `sync_queue`
--
ALTER TABLE `sync_queue`
  MODIFY `id_sync` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `user`
--
ALTER TABLE `user`
  MODIFY `id_user` int NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `desain`
--
ALTER TABLE `desain`
  ADD CONSTRAINT `desain_ibfk_1` FOREIGN KEY (`id_pesanan`) REFERENCES `pesanan` (`id_pesanan`) ON DELETE CASCADE,
  ADD CONSTRAINT `desain_ibfk_2` FOREIGN KEY (`id_designer`) REFERENCES `user` (`id_user`) ON DELETE RESTRICT,
  ADD CONSTRAINT `desain_ibfk_3` FOREIGN KEY (`id_status_desain`) REFERENCES `status_desain` (`id_status_desain`) ON DELETE RESTRICT;

--
-- Constraints for table `detail_pesanan`
--
ALTER TABLE `detail_pesanan`
  ADD CONSTRAINT `detail_pesanan_ibfk_1` FOREIGN KEY (`id_pesanan`) REFERENCES `pesanan` (`id_pesanan`) ON DELETE CASCADE,
  ADD CONSTRAINT `detail_pesanan_ibfk_2` FOREIGN KEY (`id_layanan`) REFERENCES `jenis_layanan` (`id_layanan`) ON DELETE RESTRICT;

--
-- Constraints for table `kendala_produksi`
--
ALTER TABLE `kendala_produksi`
  ADD CONSTRAINT `kendala_produksi_ibfk_1` FOREIGN KEY (`id_produksi`) REFERENCES `produksi` (`id_produksi`) ON DELETE CASCADE,
  ADD CONSTRAINT `kendala_produksi_ibfk_2` FOREIGN KEY (`id_kendala_type`) REFERENCES `master_kendala` (`id_kendala_type`) ON DELETE SET NULL,
  ADD CONSTRAINT `kendala_produksi_ibfk_3` FOREIGN KEY (`dilaporkan_oleh`) REFERENCES `user` (`id_user`) ON DELETE RESTRICT;

--
-- Constraints for table `laporan`
--
ALTER TABLE `laporan`
  ADD CONSTRAINT `laporan_ibfk_1` FOREIGN KEY (`generated_by`) REFERENCES `user` (`id_user`) ON DELETE RESTRICT;

--
-- Constraints for table `log_aktivitas`
--
ALTER TABLE `log_aktivitas`
  ADD CONSTRAINT `log_aktivitas_ibfk_1` FOREIGN KEY (`id_user`) REFERENCES `user` (`id_user`) ON DELETE CASCADE;

--
-- Constraints for table `pembayaran`
--
ALTER TABLE `pembayaran`
  ADD CONSTRAINT `pembayaran_ibfk_1` FOREIGN KEY (`id_pesanan`) REFERENCES `pesanan` (`id_pesanan`) ON DELETE CASCADE,
  ADD CONSTRAINT `pembayaran_ibfk_2` FOREIGN KEY (`id_metode`) REFERENCES `metode_pembayaran` (`id_metode`) ON DELETE RESTRICT,
  ADD CONSTRAINT `pembayaran_ibfk_3` FOREIGN KEY (`verified_by`) REFERENCES `user` (`id_user`) ON DELETE SET NULL;

--
-- Constraints for table `pesanan`
--
ALTER TABLE `pesanan`
  ADD CONSTRAINT `pesanan_ibfk_1` FOREIGN KEY (`id_pelanggan`) REFERENCES `pelanggan` (`id_pelanggan`) ON DELETE RESTRICT,
  ADD CONSTRAINT `pesanan_ibfk_2` FOREIGN KEY (`id_user_admin`) REFERENCES `user` (`id_user`) ON DELETE RESTRICT,
  ADD CONSTRAINT `pesanan_ibfk_3` FOREIGN KEY (`id_status`) REFERENCES `status_pesanan` (`id_status`) ON DELETE RESTRICT;

--
-- Constraints for table `produksi`
--
ALTER TABLE `produksi`
  ADD CONSTRAINT `produksi_ibfk_1` FOREIGN KEY (`id_pesanan`) REFERENCES `pesanan` (`id_pesanan`) ON DELETE CASCADE,
  ADD CONSTRAINT `produksi_ibfk_2` FOREIGN KEY (`id_operator`) REFERENCES `user` (`id_user`) ON DELETE RESTRICT;

--
-- Constraints for table `revisi_desain`
--
ALTER TABLE `revisi_desain`
  ADD CONSTRAINT `revisi_desain_ibfk_1` FOREIGN KEY (`id_desain`) REFERENCES `desain` (`id_desain`) ON DELETE CASCADE,
  ADD CONSTRAINT `revisi_desain_ibfk_2` FOREIGN KEY (`direvisi_oleh`) REFERENCES `user` (`id_user`) ON DELETE RESTRICT;

--
-- Constraints for table `sync_queue`
--
ALTER TABLE `sync_queue`
  ADD CONSTRAINT `sync_queue_ibfk_1` FOREIGN KEY (`created_by`) REFERENCES `user` (`id_user`) ON DELETE CASCADE;

--
-- Constraints for table `user`
--
ALTER TABLE `user`
  ADD CONSTRAINT `user_ibfk_1` FOREIGN KEY (`id_role`) REFERENCES `role` (`id_role`) ON DELETE RESTRICT;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
