export interface ProjectVO extends BaseEntity {
  id: number | string;
  projectCode: string;
  projectName: string;
  deptId: number | string;
  deptName: string;
  leader: string;
  budget: number;
  startDate: string;
  endDate: string;
  status: number;
  remark: string;
}

export interface ProjectForm {
  id: number | string | undefined;
  projectCode: string;
  projectName: string;
  deptId: number | string | undefined;
  leader: string;
  budget: number | undefined;
  startDate: string;
  endDate: string;
  status: number;
  remark: string;
}

export interface ProjectQuery extends PageQuery {
  projectCode: string;
  projectName: string;
  deptId: number | string | undefined;
  leader: string;
  status: number | undefined;
}
